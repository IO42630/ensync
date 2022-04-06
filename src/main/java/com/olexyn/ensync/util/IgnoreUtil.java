package com.olexyn.ensync.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IgnoreUtil {

    public static List<String> IGNORE = new ArrayList<>();


    public static boolean noIgnore(Path path) {
        for (var line : IGNORE) {
            line = line
                .replace("/", "")
                .replace("\\", "");
            var pathX = path.toString()
                .replace("/", "")
                .replace("\\", "");
            if (pathX.contains(line)) {
                return false;
            }
        }
        return  true;
    }
}
