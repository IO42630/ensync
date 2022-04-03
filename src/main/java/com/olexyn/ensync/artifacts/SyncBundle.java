package com.olexyn.ensync.artifacts;


import com.olexyn.ensync.Tools;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A SyncMap is a map of SyncDirectories. <br>
 * It synchronizes the SyncDirectories it contains.
 */
public class SyncBundle {

    public String name;
    public Map<Path, SyncDirectory> syncDirectories = new HashMap<>();

    Tools tools = new Tools();

    /**
     * @see SyncBundle
     */
    public SyncBundle(String name) {
        this.name = name;
    }

    public Collection<SyncDirectory> getSyncDirectories() {
        return syncDirectories.values();
    }

    /**
     * Creates a new SyncDirectory. <p>
     * Adds the created SyncDirectory to this SyncBundle.
     *
     * @param path the path from which the SyncDirectory is created.
     * @see SyncDirectory
     */
    public void addDirectory(Path path) {
        if (path.toFile().isDirectory()) {
            syncDirectories.put(path, new SyncDirectory(path, this));
        }
    }

    public void removeDirectory(String realPath) {
        syncDirectories.remove(realPath);
    }

}
