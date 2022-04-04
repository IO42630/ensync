package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.StateFile;
import com.olexyn.ensync.artifacts.SyncDirectory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


public class Flow implements Runnable {

    private static final Logger LOGGER = LogUtil.get(Flow.class);

    public static final long POLLING_PAUSE = 400;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() {
        Thread worker = new Thread(this);
        worker.start();
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {

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
                LOGGER.info("Pausing... for " + POLLING_PAUSE + "ms.");
                Thread.sleep(POLLING_PAUSE);
            } catch (InterruptedException ignored) {
                LOGGER.info("Thread interrupted.");
            }
        }
    }

    /**
     *
     */
    private void doSyncDirectory(SyncDirectory sd) {
        LOGGER.info("DO SYNC DIRECTORY");
        sd.readFileSystem();


        sd.fillListOfLocallyCreatedFiles();
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
                    LOGGER.info("READ-STATE-FILE");
                } else {
                    sd.writeStateFile(new StateFile(sd.directoryPath));
                }
            }
        });
    }

}
