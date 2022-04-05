package com.olexyn.ensync.lock;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class FcState {

    Path path;
    FileChannel fc;
    boolean locked;

    public FcState(Path path, FileChannel fc, boolean locked) {
        this.path = path;
        this.fc = fc;
        this.locked = locked;
    }

    public FileChannel getFc() {
        return fc;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isUnlocked() {
        return !isLocked();
    }

    public Path getPath() {
        return path;
    }

}