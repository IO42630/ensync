package com.olexyn.ensync.artifacts;


import com.olexyn.ensync.Tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * What is a SyncMap?
 * A SyncDirectory is an occurrence of a particular directory somewhere across the filesystems.
 * A SyncMap is the set of such SyncDirectories. The purpose of the SyncMap is to help syncronize the SyncDirectories.
 */
public class SyncMap {

    public String name;
    public int syncInterval = 3600;
    public Map<String, SyncDirectory> syncDirectories = new HashMap<>();

    Tools tools = new Tools();

    /**
     * @see SyncMap
     */
    public SyncMap(String name) {
        this.name = name;
    }

    /**
     * Creates a new Syncdirectory. <p>
     * Adds the created SyncDirectory to this SyncMap.
     *
     * @param realPath the path from which the SyncDirectory is created.
     * @see SyncDirectory
     */
    public void addDirectory(String realPath) {
        if (new File(realPath).isDirectory()) {
            syncDirectories.put(realPath, new SyncDirectory(realPath, this));
        }
    }

    public void removeDirectory(String realPath) {
        syncDirectories.remove(realPath);
    }


}