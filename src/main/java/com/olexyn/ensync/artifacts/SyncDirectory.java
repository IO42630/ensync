package com.olexyn.ensync.artifacts;

import com.olexyn.ensync.Tools;
import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.ensync.util.FileMove;
import com.olexyn.ensync.util.HashUtil;
import com.olexyn.ensync.util.IgnoreUtil;
import com.olexyn.min.log.LogU;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.olexyn.ensync.artifacts.Constants.EMPTY;
import static com.olexyn.ensync.artifacts.Constants.RECORD_SEPARATOR;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_COPY_COULD_NOT_HASH;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_COPY_NOT_A_FILE;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_COPY_OTHER_IS_NEWER;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_COPY_SAME_AGE;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_DELETE_OTHER_IS_NEWER;
import static com.olexyn.ensync.artifacts.OpsResultType.IGNORE_DELETE_OTHER_NOT_FOUND;
import static com.olexyn.ensync.artifacts.OpsResultType.OK_COPY;
import static com.olexyn.ensync.artifacts.OpsResultType.OK_DELETE;
import static com.olexyn.ensync.artifacts.OpsResultType.OK_DROPPED_AGE_FROM;
import static com.olexyn.ensync.artifacts.OpsResultType.OK_DROPPED_AGE_TO;
import static com.olexyn.ensync.artifacts.OpsResultType.WARN_COULD_NOT_COPY;
import static com.olexyn.ensync.artifacts.OpsResultType.WARN_COULD_NOT_CREATE_PARENT;
import static com.olexyn.ensync.artifacts.OpsResultType.WARN_COULD_NOT_DELETE;

/**
 * A SyncDirectory is a singular occurrence of a directory in the filesystems.
 */
public class SyncDirectory {

    private final SyncBundle syncMap;
    public Path directoryPath;
    Tools tools = new Tools();

    /**
     * Create a SyncDirectory from realPath.
     *
     * @see SyncBundle
     */
    public SyncDirectory(Path directoryPath, SyncBundle syncMap) {
        this.directoryPath = directoryPath;
        this.syncMap = syncMap;
    }


    /**
     * Read the current state of the file system.
     */
    public Map<String, SyncFile> readFileSystem() {
        return getFiles()
            .map(file -> new SyncFile(this, file.getAbsolutePath()))
            .collect(Collectors.toMap(
                SyncFile::getRelativePath,
                syncFile -> syncFile
            ));
    }

    /**
     * READ the contents of Record to Map.
     */
    public Map<String, RecordFile> readRecord() {
        Map<String, RecordFile> filemap = new HashMap<>();
        var record = new Record(directoryPath);

        var lines = Tools.fileToLines(LockKeeper.getFc(record.getPath()));

        for (String line : lines) {
            // this is a predefined format: "<modification-time>RECORD_SEPARATOR<relative-path>"
            var lineArr = line.split(RECORD_SEPARATOR);
            long modTime = Long.parseLong(lineArr[0]);
            String sFilePath = lineArr[1];
            String absolutePath = directoryPath + sFilePath;
            RecordFile recordFile = new RecordFile(this, absolutePath);

            recordFile.setTimeModifiedFromRecord(modTime);
            if (IgnoreUtil.noIgnore(recordFile.toPath())) {
                filemap.put(sFilePath, recordFile);
            }
        }
        return filemap;
    }


    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, SyncFile> fillListOfLocallyCreatedFiles(Map<String, SyncFile> listFileSystem, Record record) {
        var listCreated = Tools.setMinus(listFileSystem.keySet(), record.getFiles().keySet());
        return listCreated.stream().collect(Collectors.toMap(key -> key, listFileSystem::get));
    }

    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, SyncFile> makeListOfLocallyDeletedFiles(Map<String, SyncFile> listFileSystem, Record record) {
        var listDeleted = Tools.setMinus(record.getFiles().keySet(), listFileSystem.keySet());
        return listDeleted.stream().collect(Collectors.toMap(key -> key, key -> record.getFiles().get(key)));
    }

    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, SyncFile> makeListOfLocallyModifiedFiles(Map<String, SyncFile> listFileSystem, Record record) {
        return listFileSystem.entrySet().stream().filter(
            fileEntry -> {
                String fileKey = fileEntry.getKey();
                SyncFile file = fileEntry.getValue();
                if (file.isDirectory()) { return false; } // no need to modify Directories, the Filesystem will do that, if a File changed.
                boolean isKnown = record.getFiles().containsKey(fileKey); // If KEY exists in OLD , thus FILE was NOT created.
                boolean isModified = file.lastModified() > record.lastModified(fileKey);
                return isKnown && isModified;
            }
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * QUERY state of the filesystem at realPath.
     * WRITE the state of the filesystem to file.
     */
    public void writeRecord(Record record) {
        List<String> outputList = new ArrayList<>();
        getFiles().forEach(
            file -> {
                String relativePath = file.getAbsolutePath()
                    .replace(record.getTargetPath().toString(), EMPTY);
                var line = String.join(
                    RECORD_SEPARATOR,
                    String.valueOf(file.lastModified()),
                    relativePath
                );
                outputList.add(line);
            });

        LogU.infoPlain("Writing " + outputList.size() + " files to Record: " + record.getPath());
        tools.writeStringListToFile(record.getPath().toString(), outputList);
    }



    private Stream<File> getFiles() {
        try {
            return Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> IgnoreUtil.noIgnore(file.toPath()))
                .filter(file -> !file.getName().equals(Constants.STATE_FILE_NAME));
        } catch (IOException e) {
            LogU.warnPlain("Could walk the file tree : Record will be empty.");
            return Stream.empty();
        }
    }

    public Map<OpsResultType, List<FileMove>> doCreateOpsOnOtherSDs(Map<String, SyncFile> listCreated) {
        var opsResults = initOpsResults();
        for (var createdFile : listCreated.values()) {
            for (var otherFile : otherFiles(createdFile)) {
                var result = writeFileIfNewer(createdFile, otherFile);
                opsResults.get(result).add(new FileMove(createdFile.getAbsolutePath(), otherFile.getAbsolutePath()));
            }
        }
        return opsResults;
    }

    public Map<OpsResultType, List<FileMove>> doDeleteOpsOnOtherSDs(Map<String, SyncFile> listDeleted) {
        var opsResults = initOpsResults();
        for (var deletedFile : listDeleted.values()) {
            for (var otherFile : otherFiles(deletedFile)) {
                var result = deleteFileIfNewer(deletedFile, otherFile);
                opsResults.get(result).add(new FileMove(deletedFile.getAbsolutePath(), otherFile.getAbsolutePath()));
            }
        }
        return opsResults;
    }

    private Map<OpsResultType, List<FileMove>> initOpsResults() {
        Map<OpsResultType, List<FileMove>> opsResults = new HashMap<>();
        Arrays.stream(OpsResultType.values())
            .forEach(opsResultType -> opsResults.put(opsResultType, new ArrayList<>()));
        return opsResults;
    }

    public Map<OpsResultType, List<FileMove>> doModifyOpsOnOtherSDs(Map<String, SyncFile> listModified) {
        var opsResults = initOpsResults();
        for (var modifiedFile : listModified.values()) {
            for (var otherFile : otherFiles(modifiedFile)) {
                var result = writeFileIfNewer(modifiedFile, otherFile);
                opsResults.get(result).add(new FileMove(modifiedFile.getAbsolutePath(), otherFile.getAbsolutePath()));
            }
        }
        return opsResults;
    }

    private Collection<SyncFile> otherFiles(SyncFile thisFile) {
        return syncMap.getSyncDirectories().stream()
            .filter(sd -> !this.equals(sd))
            .map(thisFile::otherFile)
            .collect(Collectors.toList());
    }

    /**
     * Delete other file if this file is newer.
     * Here the >= is crucial, since otherFile might have == modified,
     * but in that case we still want to delete both files.
     */
    private OpsResultType deleteFileIfNewer(SyncFile thisFile, SyncFile otherFile) {
        if (!otherFile.exists()) { return IGNORE_DELETE_OTHER_NOT_FOUND; }
        if (thisFile.lastModified() >= otherFile.lastModified()) {
            try {
                Files.delete(otherFile.toPath());
                return OK_DELETE;
            } catch (IOException e) {
                e.printStackTrace();
                return WARN_COULD_NOT_DELETE;
            }
        }
        return IGNORE_DELETE_OTHER_IS_NEWER;
    }

    /**
     * Overwrite other file if this file is newer.
     */
    private OpsResultType writeFileIfNewer(SyncFile thisFile, SyncFile otherFile) {
        if (!thisFile.isFile()) { return IGNORE_COPY_NOT_A_FILE; }
        if (otherFile.exists()) {
            var thisHash = HashUtil.getHash(thisFile.toPath());
            var otherHash = HashUtil.getHash(otherFile.toPath());
            if (thisHash == null || otherHash == null) { return IGNORE_COPY_COULD_NOT_HASH; }
            if (thisHash.equals(otherHash)) {
                return dropAge(thisFile, otherFile);
            } else if (thisFile.lastModified() <= otherFile.lastModified()) {
                return IGNORE_COPY_OTHER_IS_NEWER;
            }
        }
        return copyFile(thisFile, otherFile);
    }

    private OpsResultType dropAge(SyncFile thisFile, SyncFile otherFile) {
        if (thisFile.lastModified() == otherFile.lastModified()) { return IGNORE_COPY_SAME_AGE; }
        if (thisFile.lastModified() < otherFile.lastModified()) {
            otherFile.setLastModified(thisFile.lastModified());
            return OK_DROPPED_AGE_TO;
        } else {
            thisFile.setLastModified(otherFile.lastModified());
            return OK_DROPPED_AGE_FROM;
        }
    }

    private OpsResultType copyFile(SyncFile thisFile, SyncFile otherFile) {
        if (!otherFile.getParentFile().exists()) {
            try { FileUtils.createParentDirectories(otherFile); }
            catch (IOException e) { return WARN_COULD_NOT_CREATE_PARENT; }
        }
        var thisFc = LockKeeper.getFc(thisFile.toPath());
        var otherFc = LockKeeper.getFc(otherFile.toPath());
        try (
            var is = Channels.newInputStream(thisFc);
            var os = Channels.newOutputStream(otherFc)
        ) {
            copyStream(is, os);
        } catch (Exception e) {
            e.printStackTrace();
            return WARN_COULD_NOT_COPY;
        }
        otherFile.setLastModified(thisFile.lastModified());
        return OK_COPY;
    }

    public static void copyStream(InputStream input, OutputStream output)
        throws IOException {
        byte[] buffer = new byte[262144];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

}


