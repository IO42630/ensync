package com.olexyn.ensync.artifacts;

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
}
