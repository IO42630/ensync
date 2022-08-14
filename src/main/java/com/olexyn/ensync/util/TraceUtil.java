package com.olexyn.ensync.util;

import com.olexyn.ensync.artifacts.SyncFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TraceUtil {

    public static Set<String> fileNameSet(Map<String, SyncFile> batchMap) {
        return batchMap.keySet().stream()
            .map(TraceUtil::fileName)
            .collect(Collectors.toSet());
    }

    private static String fileName(String pathStr) {
        return Path.of(pathStr).getFileName().toString();
    }

}
