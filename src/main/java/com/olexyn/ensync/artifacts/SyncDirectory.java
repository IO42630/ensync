package com.olexyn.ensync.artifacts;

import com.olexyn.ensync.Execute;
import com.olexyn.ensync.Flow;
import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A SyncDirectory is a singular occurrence of a directory in the filesystems.
 */
public class SyncDirectory {

    private static final Logger LOGGER = LogUtil.get(SyncDirectory.class);

    private String flowState;
    private SyncDirectory thisSD = this;


    private final SyncBundle syncMap;
    public String path = null;

    public Map<String, SyncFile> listCreated = new HashMap<>();
    public Map<String, SyncFile> listDeleted = new HashMap<>();
    public Map<String, SyncFile> listModified = new HashMap<>();


    Tools tools = new Tools();
    Execute x = new Execute();

    /**
     * Create a SyncDirectory from realPath.
     *
     * @see SyncBundle
     */
    public SyncDirectory(String path, SyncBundle syncMap) {

        this.path = path;
        this.syncMap = syncMap;

    }


    /**
     * Get the current state by using the `find` command.
     */
    public Map<String, SyncFile> readStateFromFS() {
        //NOTE that the SFile().lastModifiedOld is not set here, so it is 0 by default.
        Map<String, SyncFile> filemap = new HashMap<>();

        Execute.TwoBr find = x.execute(new String[]{
            "find",
            path
        });

        List<String> pathList = tools.brToListString(find.output);

        for (String filePath : pathList) {
            SyncFile file = new SyncFile(this, filePath);

            filemap.put(filePath, file);
        }
        return filemap;
    }


    /**
     * READ the contents of StateFile to Map.
     */
    public Map<String, SyncFile> readStateFile() {
        Map<String, SyncFile> filemap = new HashMap<>();
        var stateFile = new StateFile(path);
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

        Map<String, SyncFile> fromA = readStateFile();
        Map<String, SyncFile> substractB = readStateFromFS();

        Map<String, SyncFile> listDeleted = tools.mapMinus(fromA, substractB);

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
    public Map<String, SyncFile> makeListOfLocallyModifiedFiles() {

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
        return listModified;
    }


    /**
     * QUERY state of the filesystem at realPath.
     * WRITE the state of the filesystem to file.
     */
    public void writeStateFile(StateFile stateFile) {
        List<String> outputList = new ArrayList<>();

        try {
            Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> !file.getName().equals(Constants.STATE_FILE_NAME))
                .forEach(file -> {
                    String relativePath = file.getAbsolutePath()
                        .replace(stateFile.getTargetPath(), "");
                    outputList.add("" + file.lastModified() + " " + relativePath);
                });
        } catch (IOException e) {
            LOGGER.severe("Could walk the file tree : StateFile will be empty.");
        }

        tools.writeStringListToFile(stateFile.getPath(), outputList);
    }


    private class Info {

        private String thisFilePath;
        private String otherFilePath;
        private String otherParentPath;
        private File otherParentFile;


        private Info(SyncDirectory thisSD, SyncFile sFile, SyncDirectory otherSD) {
            // Example:
            //  syncDirectory /foo
            //  otherSyncDirectory /bar
            //  createdFile  /foo/hello/created-file.gif
            //  relativePath /hello/created-file.gif
            String relativePath = sFile.getPath().replace(thisSD.path, "");
            this.thisFilePath = sFile.getPath();
            this.otherFilePath = otherSD.path + relativePath;
            File otherFile = new File(otherFilePath);

            this.otherParentPath = otherFile.getParent();
            this.otherParentFile = new File(otherParentPath);


        }
    }


    public void doCreateOpsOnOtherSDs() {

        for (var entry : listCreated.entrySet()) {
            SyncFile createdFile = entry.getValue();

            for (var otherEntry : syncMap.syncDirectories.entrySet()) {
                SyncDirectory otherSD = otherEntry.getValue();

                if (this.equals(otherSD)) { continue; }

                Info info = new Info(this, createdFile, otherSD);

                writeFileIfNewer(info, createdFile, otherSD);
            }
        }
    }


    /**
     *
     */
    public void doDeleteOpsOnOtherSDs() {

        for (var entry : listDeleted.entrySet()) {
            SyncFile deletedFile = entry.getValue();

            for (var otherEntry : syncMap.syncDirectories.entrySet()) {
                SyncDirectory otherSD = otherEntry.getValue();

                if (this.equals(otherSD)) { continue; }

                Info info = new Info(thisSD, deletedFile, otherSD);
                deleteFile(info, thisSD, deletedFile, otherSD);


            }
        }
    }

    private void deleteFile(Info info, SyncDirectory thisSD, SyncFile thisFile, SyncDirectory otherSD) {

        SyncFile otherFile = new SyncFile(otherSD, otherSD.path + thisFile.relativePath);

        if (!otherFile.exists()) { return;}

        // if the otherFile was created with ensync it will have the == TimeModified.
        long thisFileTimeModified = thisFile.getTimeModified();
        long otherFileTimeModified = otherFile.getTimeModified();

        if (thisFile.getTimeModified() >= otherFile.getTimeModified()) {
            List<String> cmd = List.of("rm", "-r", info.otherFilePath);
            x.execute(cmd);
        }
    }


    public void doModifyOpsOnOtherSDs() {

        for (var entry : listModified.entrySet()) {
            SyncFile modifiedFile = entry.getValue();

            for (var otherEntry : syncMap.syncDirectories.entrySet()) {
                SyncDirectory otherSD = otherEntry.getValue();

                if (this.equals(otherSD)) { continue; }

                Info info = new Info(this, modifiedFile, otherSD);

                writeFileIfNewer(info, modifiedFile, otherSD);
            }
        }
    }

    /***
     *
     * @param info
     * @param thisFile
     * @param otherSD
     */
    private void writeFileIfNewer(Info info, SyncFile thisFile, SyncDirectory otherSD) {

        SyncFile otherFile = new SyncFile(otherSD, otherSD.path + thisFile.relativePath);

        if (otherFile.exists() && thisFile.getTimeModified() < otherFile.getTimeModified()) { return;}

        if (thisFile.isDirectory() && !otherFile.exists()) {
            List<String> cmd = List.of("mkdir", "-p", info.otherFilePath);
            x.execute(cmd);
            return;
        }

        if (thisFile.isFile()) {

            if (!info.otherParentFile.exists()) {
                makeParentChain(otherFile, thisFile);
            }

            List<String> cmd = List.of("cp", "-p", info.thisFilePath, info.otherFilePath);
            x.execute(cmd);
            copyModifDate(thisFile.getParentFile(), otherFile.getParentFile());
        }
    }

    /**
     * @param otherFile
     * @param thisFile
     */
    private void makeParentChain(File otherFile, File thisFile) {
        try {
            File otherParent = new File(otherFile.getParent());
            File thisParent = new File(thisFile.getParent());

            if (!otherParent.exists()) {
                makeParentChain(otherParent, thisParent);
                makeParentChain(otherFile, thisFile);

            } else if (thisFile.isDirectory()) {

                List<String> cmd = List.of("mkdir", otherFile.getPath());
                x.execute(cmd);


                cmd = List.of("stat", "--format", "%y", thisFile.getPath());


                String mDate = x.execute(cmd).output.readLine();


                cmd = List.of("touch", "-m", "--date=" + mDate, otherFile.getPath());
                String error = x.execute(cmd).error.readLine();
                int br = 0;


            }
        } catch (Exception ignored) {}
    }


    private void copyModifDate(File fromFile, File toFile) {
        try {
            List<String> cmd = List.of("stat", "--format", "%y", fromFile.getPath());
            String mDate = x.execute(cmd).output.readLine();

            cmd = List.of("touch", "-m", "--date=" + mDate, toFile.getPath());
            x.execute(cmd);
        } catch (Exception ignored) {}
    }

}


