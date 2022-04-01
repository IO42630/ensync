package com.olexyn.ensync.artifacts;

import java.util.Collection;
import java.util.HashMap;

public class DataRoot {

    private static HashMap<String, SyncBundle> mapOfSyncMaps;

    private DataRoot() {}

    public static HashMap<String, SyncBundle> get() {

        if (mapOfSyncMaps == null) {
            mapOfSyncMaps = new HashMap<>();
        }
        return mapOfSyncMaps;

    }

    public static Collection<SyncBundle> getSyncBundles() {
        return get().values();
    }

}
