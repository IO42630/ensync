package com.olexyn.ensync.artifacts;


import com.olexyn.ensync.Tools;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A SyncMap is a map of SyncDirectories. <br>
 * It synchronizes the SyncDirectories it contains.
 */
public class SyncBundle {

    public String name;
    public List<SyncDirectory> syncDirectories = new ArrayList<>();

    Tools tools = new Tools();

    /**
     * @see SyncBundle
     */
    public SyncBundle(String name) {
        this.name = name;
    }

    public Collection<SyncDirectory> getSyncDirectories() {
        return syncDirectories;
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
            syncDirectories.add(new SyncDirectory(path, this));
        }
    }


}
