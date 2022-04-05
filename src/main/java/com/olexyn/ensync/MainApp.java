package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;

import com.olexyn.ensync.lock.LockUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class MainApp {

    final public static Flow FLOW = new Flow();
    public static List<String> IGNORE = new ArrayList<>();
    final private static Tools TOOLS = new Tools();

    public static void main(String[] args) throws JSONException {

        var configPath = Path.of(System.getProperty("user.dir") + "/src/main/resources/config.json");
        String configString = Tools.fileToString(LockUtil.lockFile(configPath).getFc());
        JSONObject dataRoot = new JSONObject(configString).getJSONObject("dataRoot");
        for (String bundleKey : dataRoot.keySet()) {
            SyncBundle syncBundle = new SyncBundle(bundleKey);
            dataRoot.getJSONArray(bundleKey).toList()
                .forEach(
                    directoryPath -> syncBundle.addDirectory(Path.of(directoryPath.toString()))
                );

            DataRoot.get().put(bundleKey, syncBundle);
        }

        var ignorePath = Path.of(System.getProperty("user.dir") + "/src/main/resources/syncignore");
        IGNORE = Tools.fileToLines(LockUtil.lockFile(ignorePath).getFc());
        FLOW.start();
    }
}
