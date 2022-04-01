package com.olexyn.ensync.artifacts;

import com.olexyn.ensync.Execute;
import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A SyncDirectory is a singular occurrence of a directory in the filesystems.
 */
public class SyncDirectory {

    private static final Logger LOGGER = LogUtil.get(SyncDirectory.class);

    private final SyncBundle syncMap;
    public String directoryPath;

    public final Map<String, SyncFile> listCreated = new HashMap<>();
    public final Map<String, SyncFile> listDeleted = new HashMap<>();
    public final Map<String, SyncFile> listModified = new HashMap<>();


    Tools tools = new Tools();
    Execute x = new Execute();

    /**
     * Create a SyncDirectory from realPath.
     *
     * @see SyncBundle
     */
    public SyncDirectory(String directoryPath, SyncBundle syncMap) {

        this.directoryPath = directoryPath;
        this.syncMap = syncMap;

    }


    /**
     * Get the current state by using the `find` command.
     */
    public Map<String, SyncFile> readStateFromFS() {
        //NOTE that the SyncFile().lastModifiedOld is not set here, so it is 0 by default.
        Map<String, SyncFile> filemap = new HashMap<>();
        getFiles()
            .map(file -> new SyncFile(this, file.getAbsolutePath()))
            .forEach(file -> filemap.put(file.getAbsolutePath(), file)
        );
        return filemap;
    }


    /**
     * READ the contents of StateFile to Map.
     */
    public Map<String, SyncFile> readStateFile() {
        Map<String, SyncFile> filemap = new HashMap<>();
        var stateFile = new StateFile(directoryPath);
        List<String> lines = tools.fileToLines(new File(stateFile.getPath()));

        for (String line : lines) {
            // this is a predefined format: "modification-time path"
            String modTimeString = line.split(" ")[0];
            long modTime = Long.parseLong(modTimeString);

            String sFilePath = line.replace(modTimeString + " ", "");
            SyncFile sfile = new SyncFile(this, sFilePath);

            sfile.setTimeModifiedFromStateFile(modTime);

            filemap.put(sFilePath, sfile);
        }
        return filemap;
    }


    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, SyncFile> makeListOfLocallyCreatedFiles() {

        Map<String, SyncFile> fromA = readStateFromFS();
        Map<String, SyncFile> substractB = readStateFile();

        return tools.mapMinus(fromA, substractB);
    }


    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public Map<String, SyncFile> makeListOfLocallyDeletedFiles() {

        var fromA = readStateFile();
        var substractB = readStateFromFS();

        var listDeleted = tools.mapMinus(fromA, substractB);

        Map<String, SyncFile> swap = new HashMap<>();


        for (var entry : listDeleted.entrySet()) {

            String key = entry.getKey();
            String parentKey = entry.getValue().getParent();

            if (listDeleted.containsKey(parentKey) || swap.containsKey(parentKey)) {
                swap.put(key, listDeleted.get(key));
            }
        }

        return tools.mapMinus(listDeleted, swap);
    }


    /**
     * Compare the OLD and NEW pools.
     * List is cleared and created each time.
     */
    public void makeListOfLocallyModifiedFiles() {


        Map<String, SyncFile> listModified = new HashMap<>();

        Map<String, SyncFile> stateFileMap = readStateFile();

        for (var freshFileEntry : readStateFromFS().entrySet()) {

            String freshFileKey = freshFileEntry.getKey();
            SyncFile freshFile = freshFileEntry.getValue();

            if (freshFile.isDirectory()) { continue;} // no need to modify Directories, the Filesystem will do that, if a File changed.

            // If KEY exists in OLD , thus FILE was NOT created.
            boolean oldFileExists = stateFileMap.containsKey(freshFileKey);
            boolean fileIsFresher = freshFile.getTimeModified() > freshFile.getTimeModifiedFromStateFile();

            if (oldFileExists && fileIsFresher) {
                listModified.put(freshFileKey, freshFile);
            }
        }
    }


    /**
     * QUERY state of the filesystem at realPath.
     * WRITE the state of the filesystem to file.
     */
    public void writeStateFile(StateFile stateFile) {
        List<String> outputList = new ArrayList<>();
        getFiles().forEach(
            file -> {
                String relativePath = file.getAbsolutePath()
                    .replace(stateFile.getTargetPath(), "");
                outputList.add("" + file.lastModified() + " " + relativePath);
            });
        tools.writeStringListToFile(stateFile.getPath(), outputList);
    }

    private Stream<File> getFiles() {
        try {
            return Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> !file.getName().equals(Constants.STATE_FILE_NAME));
        } catch (IOException e) {
            LOGGER.severe("Could walk the file tree : StateFile will be empty.");
            return Stream.empty();
        }
    }

    public void doCreateOpsOnOtherSDs() {
        for (var createdFile : listCreated.values()) {
            for (var otherFile : otherFiles(createdFile)) {
                writeFileIfNewer(createdFile, otherFile);
            }
        }
    }

    public void doDeleteOpsOnOtherSDs() {
        for (var deletedFile : listDeleted.values()) {
            for (var otherFile : otherFiles(deletedFile)) {
                deleteFileIfNewer(deletedFile, otherFile);
            }
        }
    }

    public void doModifyOpsOnOtherSDs() {
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
     */
    private void deleteFileIfNewer(SyncFile thisFile, SyncFile otherFile) {
        if (!otherFile.exists()) { return; }
        // if the otherFile was created with ensync it will have the == TimeModified.
        if (thisFile.getTimeModified() >= otherFile.getTimeModified()) {
            try {
                Files.delete(Path.of(otherFile.getPath()));
            } catch (IOException e) {
                LOGGER.severe("Could not delete file.");
            }
        }
    }

    /**
     * Overwrite other file if this file is newer.
     */
    private void writeFileIfNewer(SyncFile thisFile, SyncFile otherFile) {
        if (otherFile.exists() && thisFile.isOlder(otherFile)) { return; }
        if (thisFile.isFile()) {
            try {
                Files.copy(
                    Path.of(thisFile.getPath()),
                    Path.of(otherFile.getPath())
                );
            } catch (IOException e) {
                LOGGER.severe("Could not copy file.");
            }
        }
    }

}


