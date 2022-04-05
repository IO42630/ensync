package com.olexyn.ensync.lock;

import com.olexyn.ensync.LogUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import  static java.util.logging.Level.INFO;

public class LockUtil {

    private static final int DEFAULT_LOCK_TRIES = 4;
    private static final long SLEEP_DURATION = 1000;

    private static final Logger LOGGER = LogUtil.get(LockUtil.class);

    public static FcState newFile(Path filePath) {
        try {
            var fc = FileChannel.open(filePath, CREATE_NEW, WRITE);
            return new FcState(filePath, fc, false);
        } catch (IOException | OverlappingFileLockException e) {
            LOGGER.log(INFO, "Could not NEW " + filePath, e);
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
                LOGGER.info("Could not lock " + filePath + " Will try " + tryCount + " times.");
                try {
                    Thread.sleep(SLEEP_DURATION);
                } catch (InterruptedException ignored) { }
                return lockFile(filePath, tryCount);
            }
            LOGGER.log(INFO, "Could not lock " + filePath, e);
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
                LOGGER.info("Could not close " + fc + " Will try " + tryCount + " times.");
                return unlockFile(filePath, fc, tryCount);
            }
            LOGGER.info("Could not unlock " + fc);
            return new FcState(filePath, null, true);
        }
    }
}
