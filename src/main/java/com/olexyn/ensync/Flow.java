package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.OpsResultType;
import com.olexyn.ensync.artifacts.Record;
import com.olexyn.ensync.artifacts.SyncDirectory;
import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.ensync.util.FileMove;
import com.olexyn.ensync.util.TraceUtil;
import com.olexyn.min.log.LogU;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class Flow implements Runnable {

    public static final long POLLING_PAUSE = 1000;
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
        LogU.infoStart("SYNC %s", sDir.directoryPath);
        var listFileSystem = sDir.readFileSystem();
        LogU.infoPlain("# FS:       %s", listFileSystem.size());
        var record = new Record(sDir.directoryPath);
        record.getFiles().putAll(sDir.readRecord());
        LogU.infoPlain("# Record:   %s", record.getFiles().size());

        var mapCreated = sDir.fillListOfLocallyCreatedFiles(listFileSystem, record);
        var mapDeleted = sDir.makeListOfLocallyDeletedFiles(listFileSystem, record);
        var mapModified = sDir.makeListOfLocallyModifiedFiles(listFileSystem, record);

        var setCreatedNames = TraceUtil.fileNameSet(mapCreated);
        var setDeletedNames = TraceUtil.fileNameSet(mapDeleted);
        var setCreatedNamesNew = Tools.setMinus(setCreatedNames, setDeletedNames);
        var setDeletedNamesFinal = Tools.setMinus(setDeletedNames, setCreatedNames);

        LogU.infoPlain("# CREATED:  %s", mapCreated.size());
        LogU.infoPlain("    thereof unique         %s", setCreatedNames.size());
        LogU.infoPlain("    thereof new            %s", setCreatedNamesNew.size());
        var createdByMove = setCreatedNames.size() - setCreatedNamesNew.size();
        LogU.infoPlain("    thereof by mv          %s", createdByMove);
        LogU.infoPlain("# DELETED:  %s", mapDeleted.size());
        LogU.infoPlain("    thereof unique         %s", setDeletedNames.size());
        LogU.infoPlain("    thereof finally        %s", setDeletedNamesFinal.size());
        var deletedByMove = setDeletedNames.size() - setDeletedNamesFinal.size();
        LogU.infoPlain("    thereof by mv          %s", deletedByMove);
        LogU.infoPlain("# MODIFIED: %s", mapModified.size());
        if (createdByMove == deletedByMove) {
            LogU.infoPlain("(created by mv == deleted by mv) -> EXECUTE OPS");
            var createResults = sDir.doCreateOpsOnOtherSDs(mapCreated);
            printResults(createResults);
            var deleteResults = sDir.doDeleteOpsOnOtherSDs(mapDeleted);
            printResults(deleteResults);
            var modifyResults = sDir.doModifyOpsOnOtherSDs(mapModified);
            printResults(modifyResults);
            sDir.writeRecord(record);
        } else {
            LogU.warnPlain("(created by mv != deleted by mv) -> ABORT");
        }
        LogU.infoEnd("SYNC %s", sDir.directoryPath);
    }

    private void printResults(Map<OpsResultType, List<FileMove>> map) {
        map.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .forEach(entry -> LogU.infoPlain("%-30s:   %s", entry.getKey().name(), entry.getValue().size()));
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
