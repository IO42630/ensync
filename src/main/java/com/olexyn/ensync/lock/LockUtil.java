package com.olexyn.ensync.lock;

import com.olexyn.min.log.LogU;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class LockUtil {

    private static final int DEFAULT_LOCK_TRIES = 4;
    private static final long SLEEP_DURATION = 1000;


    public static FcState newFile(Path filePath) {
        try {
            var fc = FileChannel.open(filePath, CREATE_NEW, WRITE);
            return new FcState(filePath, fc, false);
        } catch (IOException | OverlappingFileLockException e) {
            LogU.warnPlain("Could not NEW %s\n%s", filePath, e.getMessage());
            return new FcState(filePath, null, false);
        }
    }

    public static FcState lockFile(Path filePath) {
        return lockFile(filePath, DEFAULT_LOCK_TRIES);
    }

    public static FcState lockFile(Path filePath, int tryCount) {
        try {
            var fc = FileChannel.open(filePath, READ, WRITE);
            if (filePath.toFile().exists()) {
                fc.lock();
            }
            return new FcState(filePath, fc, true);
        } catch (IOException | OverlappingFileLockException e) {
            if (tryCount > 0) {
                tryCount--;
                LogU.warnPlain("Could not lock %s. Will try %s times.", filePath, tryCount);
                try {
                    Thread.sleep(SLEEP_DURATION);
                } catch (InterruptedException ignored) { }
                return lockFile(filePath, tryCount);
            }
            LogU.warnPlain("Could not lock %s\n%s", filePath, e.getMessage());
            return new FcState(filePath, null, false);
        }
    }

    public static FcState unlockFile(FcState fcState, int tryCount) {
        return unlockFile(fcState.getPath(), fcState.getFc(), tryCount);
    }

    public static FcState unlockFile(Path filePath, FileChannel fc, int tryCount) {
        if (fc == null) { return null; }
        try {
            fc.close();
            return new FcState(filePath, fc, false);
        } catch (IOException | OverlappingFileLockException e) {
            if (tryCount > 0) {
                tryCount--;
                LogU.warnPlain("Could not close %s. Will try %s times.", fc, tryCount);

                return unlockFile(filePath, fc, tryCount);
            }
            LogU.warnPlain("Could not unlock %s", fc);
            return new FcState(filePath, null, true);
        }
    }
}
