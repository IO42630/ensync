package com.olexyn.ensync.lock;

import com.olexyn.ensync.LogUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LockKeeper {

    private static final int TRY_COUNT = 4;

    private static final Logger LOGGER = LogUtil.get(LockKeeper.class);

    private final static Map<Path, FcState> LOCKS = new HashMap<>();

    public static boolean lockDir(Path dirPath) {
        List<FcState> fcStates;
        try {
            fcStates = Files.walk(dirPath)
                .filter(filePath -> filePath.toFile().isFile())
                .map(filePath -> LockUtil.lockFile(filePath, TRY_COUNT))
                .collect(Collectors.toList());
        } catch (IOException e) {
            return false;
        }
        LOGGER.info("LOCKED " + fcStates.size() + " files in " + dirPath);
        fcStates.forEach(fcState -> LOGGER.info("    " + fcState.getPath()));
        fcStates.forEach(fcState -> LOCKS.put(fcState.getPath(), fcState));
        return fcStates.stream().noneMatch(FcState::isUnlocked);
    }



    public static void unlockAll() {
        LOGGER.info("UNLOCKING ALL.");
            LOCKS.values().forEach(
                fcState -> LockUtil.unlockFile(fcState.getPath(), fcState.getFc(), 4)
            );
    }

    public static FileChannel getFc(Path path) {
        var fc = LOCKS.get(path).getFc();
        if (fc != null && fc.isOpen()) {
            return fc;
        }
        FcState fcState;
        if (!path.toFile().exists()) {
            fcState = LockUtil.newFile(path);
        } else {
            fcState = LockUtil.lockFile(path);
        }
        LOCKS.put(path, fcState);
        return fcState.getFc();
    }

}