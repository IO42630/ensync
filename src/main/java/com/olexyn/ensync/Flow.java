package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.StateFile;
import com.olexyn.ensync.artifacts.SyncDirectory;

import java.util.logging.Logger;


public class Flow implements Runnable {

    private static final Logger LOGGER = LogUtil.get(Flow.class);

    public static final long POLLING_PAUSE = 400;

    /**
     *
     */
    @Override
    public void run() {

        while (true) {

            synchronized(DataRoot.getSyncBundles()) {

                readOrMakeStateFile();

                DataRoot.getSyncBundles().forEach(
                    syncBundle -> {
                        var syncDirectories = syncBundle.getSyncDirectories();
                        syncDirectories.forEach(this::doSyncDirectory);
                    }
                );
            }

            try {
                System.out.println("Pausing... for " + POLLING_PAUSE + "ms.");
                Thread.sleep(POLLING_PAUSE);
            } catch (InterruptedException ignored) { }
        }
    }

    /**
     *
     */
    private void doSyncDirectory(SyncDirectory sd) {
        LOGGER.info("READ");
        sd.readStateFromFS();


        sd.makeListOfLocallyCreatedFiles();
        sd.makeListOfLocallyDeletedFiles();
        sd.makeListOfLocallyModifiedFiles();

        sd.doCreateOpsOnOtherSDs();
        sd.doDeleteOpsOnOtherSDs();
        sd.doModifyOpsOnOtherSDs();


        sd.writeStateFile(new StateFile(sd.directoryPath));
    }

    /**
     * For every single SyncDirectory try to read it's StateFile. <p>
     * If the StateFile is missing, then create a StateFile.
     */
    private void readOrMakeStateFile() {
        DataRoot.get().values().forEach(syncBundle -> {
            for (var sd : syncBundle.syncDirectories.values()) {
                var stateFile = new StateFile(sd.directoryPath);
                if (stateFile.exists()) {
                    LOGGER.info("READ-STATE-FILE-" + sd.readStateFile());
                } else {
                    sd.writeStateFile(new StateFile(sd.directoryPath));
                }
            }
        });
    }

}
