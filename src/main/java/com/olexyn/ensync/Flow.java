package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.StateFile;
import com.olexyn.ensync.artifacts.SyncDirectory;
import com.olexyn.ensync.artifacts.SyncBundle;

import java.io.File;
import java.util.Map.Entry;
import java.util.logging.Logger;



public class Flow implements Runnable {

    private static final Logger LOGGER = LogUtil.get(Flow.class);


    Tools tools = new Tools();

    public long pollingPause = 200;



    public void run() {

        while (true) {

            synchronized(DataRoot.get()) {

                readOrMakeStateFile();

                for (Entry<String, SyncBundle> syncMapEntry : DataRoot.get().entrySet()) {

                    for (Entry<String, SyncDirectory> SDEntry : syncMapEntry.getValue().syncDirectories.entrySet()) {

                        doSyncDirectory(SDEntry.getValue());
                    }
                }
            }
            try {
                System.out.println("Pausing... for " + pollingPause + "ms.");
                Thread.sleep(pollingPause);
            } catch (InterruptedException ignored) { }
        }
    }


    private void doSyncDirectory(SyncDirectory SD) {
        LOGGER.info("READ");
        SD.readStateFromFS();

        SD.listCreated = SD.makeListOfLocallyCreatedFiles();
        SD.listDeleted = SD.makeListOfLocallyDeletedFiles();
        SD.listModified = SD.makeListOfLocallyModifiedFiles();

        SD.doCreateOpsOnOtherSDs();
        SD.doDeleteOpsOnOtherSDs();
        SD.doModifyOpsOnOtherSDs();

        SD.writeStateFile(new StateFile(SD.path));
    }



    /**
     * For every single SyncDirectory try to read it's StateFile. <p>
     * If the StateFile is missing, then create a StateFile.
     */
    private void readOrMakeStateFile() {
        DataRoot.get().values().forEach(syncBundle -> {
            for (var sd : syncBundle.syncDirectories.values()) {
                var stateFile = new StateFile(sd.path);
                if (stateFile.exists()) {
                    LOGGER.info("READ-STATE-FILE-" + sd.readStateFile());
                } else {
                    sd.writeStateFile(new StateFile(sd.path));
                }
            }
        });
    }
}
