package com.olexyn.ensync.artifacts;

import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.MainApp;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.ensync.lock.LockUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.olexyn.ensync.artifacts.Constants.EMPTY;
import static com.olexyn.ensync.artifacts.Constants.RECORD_SEPARATOR;

/**
 * A SyncDirectory is a singular occurrence of a directory in the filesystems.
 */
public class SyncDirectory {

    private static final Logger LOGGER = LogUtil.get(SyncDirectory.class);
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

        var lines = tools.fileToLines(LockKeeper.getFc(record.getPath()));

        for (String line : lines) {
            // this is a predefined format: "<modification-time>RECORD_SEPARATOR<relative-path>"
            var lineArr = line.split(RECORD_SEPARATOR);
            long modTime = Long.parseLong(lineArr[0]);
            String sFilePath = lineArr[1];
            String absolutePath = directoryPath + sFilePath;
            RecordFile recordFile = new RecordFile(this, absolutePath);

            recordFile.setTimeModifiedFromRecord(modTime);
            if (!shouldIgnore(recordFile.toPath())) {
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
        var listCreated = tools.setMinus(listFileSystem.keySet(), record.getFiles().keySet());
        return listCreated.stream().collect(Collectors.toMap(key -> key, listFileSystem::get));
    }

    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, RecordFile> makeListOfLocallyDeletedFiles(Map<String, SyncFile> listFileSystem, Record record) {
        var listDeleted = tools.setMinus(record.getFiles().keySet(), listFileSystem.keySet());
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

    private String getHash(Path path) {
        var thisFc = LockKeeper.getFc(path);
        byte[] data = Tools.fileToString(thisFc).getBytes(StandardCharsets.UTF_8);
        try {
            var m = MessageDigest.getInstance("SHA256");
            m.update(data, 0, data.length);
            var i = new BigInteger(1, m.digest());
            return String.format("%1$032X", i);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.info("File not found.");
            return null;
        }
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

        LOGGER.info("Writing " + outputList.size() + " files to Record: " + record.getPath());
        tools.writeStringListToFile(record.getPath().toString(), outputList);
    }

    private boolean shouldIgnore(Path path) {
        for (var entry : MainApp.IGNORE) {
            if (path.toString().contains(entry)) {
                return true;
            }
        }
        return  false;
    }

    private Stream<File> getFiles() {
        try {
            return Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> !shouldIgnore(file.toPath()))
                .filter(file -> !file.getName().equals(Constants.STATE_FILE_NAME));
        } catch (IOException e) {
            LOGGER.severe("Could walk the file tree : Record will be empty.");
            return Stream.empty();
        }
    }

    public void doCreateOpsOnOtherSDs(Map<String, SyncFile> listCreated) {
        for (var createdFile : listCreated.values()) {
            for (var otherFile : otherFiles(createdFile)) {
                writeFileIfNewer(createdFile, otherFile);
            }
        }
    }

    public void doDeleteOpsOnOtherSDs(Map<String, RecordFile> listDeleted) {
        for (var deletedFile : listDeleted.values()) {
            for (var otherFile : otherFiles(deletedFile)) {
                deleteFileIfNewer(deletedFile, otherFile);
            }
        }
    }

    public void doModifyOpsOnOtherSDs(Map<String, SyncFile> listModified) {
        for (var modifiedFile : listModified.values()) {
            for (var otherFile : otherFiles(modifiedFile)) {
                writeFileIfNewer(modifiedFile, otherFile);
            }
        }
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
    private void deleteFileIfNewer(RecordFile thisFile, SyncFile otherFile) {
        if (!otherFile.exists()) {
            LOGGER.info("Could not delete: " + otherFile.toPath() + " not found.");
            return; }
        if (thisFile.lastModified() >= otherFile.lastModified()) {
            try {
                Files.delete(otherFile.toPath());
                LOGGER.info("Deleted: " + otherFile.toPath());
            } catch (IOException e) {
                LOGGER.info("Could not delete: " + otherFile.toPath());
            }
        }
    }

    /**
     * Overwrite other file if this file is newer.
     */
    private void writeFileIfNewer(SyncFile thisFile, SyncFile otherFile) {
        LOGGER.info("Try write from: "  + thisFile.toPath());
        LOGGER.info("            to: "  + otherFile.toPath());
        if (!thisFile.isFile()) { return; }
        if (otherFile.exists()) {
            var thisHash = getHash(thisFile.toPath());
            var otherHash = getHash(otherFile.toPath());
            if (thisHash == null || otherHash == null) { return; }
            if (thisHash.equals(otherHash)) {
                dropAge(thisFile, otherFile);
                return;
            } else if (thisFile.lastModified() <= otherFile.lastModified()) {
                LOGGER.info("Did not override due to target being newer.");
                return;
            }
        }
        copyFile(thisFile, otherFile);
    }

    private void dropAge(SyncFile thisFile, SyncFile otherFile) {
        if (thisFile.lastModified() == otherFile.lastModified()) {
            return;
        }
        if (thisFile.lastModified() < otherFile.lastModified()) {
            otherFile.setLastModified(thisFile.lastModified());
            LOGGER.info("Dropped age of: " + otherFile.toPath() + " -> " + otherFile.lastModified());
        } else {
            thisFile.setLastModified(otherFile.lastModified());
            LOGGER.info("Dropped age of:  " + thisFile.toPath() + " -> " + thisFile.lastModified());
        }
    }

    private void copyFile(SyncFile thisFile, SyncFile otherFile) {
        var thisFc = LockKeeper.getFc(thisFile.toPath());
        var otherFc = LockKeeper.getFc(otherFile.toPath());
        try (var br = Tools.reader(thisFc) ; var bw = Tools.writer(otherFc) ) {
            IOUtils.copy(br, bw);
            LOGGER.info(thisFile.toPath() + "lastModified before " + thisFile.lastModified());
            LOGGER.info(otherFile.toPath() + "lastModified before " + otherFile.lastModified());
            otherFile.setLastModified(thisFile.lastModified());
            LOGGER.info(otherFile.toPath() + "lastModified before " + otherFile.lastModified());
        } catch (IOException e) {
            LOGGER.severe("Could not copy file from: " + thisFile.toPath());
            LOGGER.severe("                      to: " + otherFile.toPath());
            e.printStackTrace();
        }
    }

}


