package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.Record;
import com.olexyn.ensync.artifacts.SyncDirectory;
import com.olexyn.ensync.lock.LockKeeper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Flow implements Runnable {

    private static final Logger LOGGER = LogUtil.get(Flow.class);

    public static final long POLLING_PAUSE = 100;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() {
        LOGGER.info("START Flow.");
        Thread worker = new Thread(this, "FLOW_WORKER");
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
                writeRecordIfMissing();
                DataRoot.getSyncBundles().forEach(
                    syncBundle -> {
                        var syncDirectories = syncBundle.getSyncDirectories();
                        var lockFail = syncDirectories.stream()
                            .map(sDir -> LockKeeper.lockDir(sDir.directoryPath))
                            .collect(Collectors.toList())
                            .contains(false);
                        if (!lockFail) {
                            syncDirectories.forEach(this::sync);
                        }
                        LockKeeper.unlockAll();
                    }
                );
            }
            try {
                LOGGER.info("Sleeping... for " + POLLING_PAUSE + "ms.");
                Thread.sleep(POLLING_PAUSE);
            } catch (InterruptedException ignored) {
                LOGGER.info("Thread interrupted.");
            }
        }
    }

    /**
     *
     */
    private void sync(SyncDirectory sDir) {
        LOGGER.info("DO SYNC " + sDir.directoryPath);
        var listFileSystem = sDir.readFileSystem();
        LOGGER.info("# of files on FS:       " + listFileSystem.size());
        var record = new Record(sDir.directoryPath);
        record.getFiles().putAll(sDir.readRecord());
        LOGGER.info("# of files on Record:   " + record.getFiles().size());
        var listCreated = sDir.fillListOfLocallyCreatedFiles(listFileSystem, record);
        LOGGER.info("# of files in Created:  " + listCreated.size());
        var listDeleted = sDir.makeListOfLocallyDeletedFiles(listFileSystem, record);
        LOGGER.info("# of files in Deleted:  " + listDeleted.size());
        var listModified = sDir.makeListOfLocallyModifiedFiles(listFileSystem, record);
        LOGGER.info("# of files in Modified: " + listModified.size());

        sDir.doCreateOpsOnOtherSDs(listCreated);
        sDir.doDeleteOpsOnOtherSDs(listDeleted);
        sDir.doModifyOpsOnOtherSDs(listModified);

        sDir.writeRecord(record);
    }

    /**
     * For every single SyncDirectory try to read it's Record. <p>
     * If the Record is missing, then create a Record.
     */
    private void writeRecordIfMissing() {
        DataRoot.get().values().forEach(syncBundle -> {
            for (var sDir : syncBundle.syncDirectories) {
                var record = new Record(sDir.directoryPath);
                if (!record.exists()) {
                    sDir.writeRecord(new Record(sDir.directoryPath));
                }
            }
        });
    }

}
