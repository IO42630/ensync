package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.Record;
import com.olexyn.ensync.artifacts.SyncDirectory;
import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.min.log.LogU;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class Flow implements Runnable {

    public static final long POLLING_PAUSE = 100;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() {
        LogU.infoStart("Flow");
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
                LogU.infoPlain("Sleeping... for %sms", POLLING_PAUSE);
                Thread.sleep(POLLING_PAUSE);
            } catch (InterruptedException ignored) {
                LogU.warnPlain("Thread interrupted.");
            }
        }
    }

    /**
     *
     */
    private void sync(SyncDirectory sDir) {
        LogU.infoPlain("DO SYNC %s", sDir.directoryPath);
        var listFileSystem = sDir.readFileSystem();
        LogU.infoPlain("# FS:       %s", listFileSystem.size());
        var record = new Record(sDir.directoryPath);
        record.getFiles().putAll(sDir.readRecord());
        LogU.infoPlain("# Record:   %s", record.getFiles().size());

        var listCreated = sDir.fillListOfLocallyCreatedFiles(listFileSystem, record);
        var listDeleted = sDir.makeListOfLocallyDeletedFiles(listFileSystem, record);
        var listModified = sDir.makeListOfLocallyModifiedFiles(listFileSystem, record);
        Tools tools = new Tools();
        int newly = tools.setMinus(listCreated.keySet(), listDeleted.keySet()).size();
        LogU.infoPlain("# Created:  %s\n   thereof newly created (not mv) %s", listCreated.size(), newly);
        LogU.infoPlain("# Deleted:  %s", listDeleted.size());
        LogU.infoPlain("# Modified: %s", listModified.size());

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
