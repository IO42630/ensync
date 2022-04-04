package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class MainApp {

    final public static Thread FLOW_THREAD = new Thread(new Flow(), "flow");
    public static List<String> IGNORE = new ArrayList<>();
    final private static Tools tools = new Tools();

    public static void main(String[] args) throws JSONException {

        String configPath = System.getProperty("user.dir") + "/src/main/resources/config.json";
        String configString = tools.fileToString(new File(configPath));
        JSONObject dataRoot = new JSONObject(configString).getJSONObject("dataRoot");
        for (String bundleKey : dataRoot.keySet()) {
            SyncBundle syncBundle = new SyncBundle(bundleKey);
            dataRoot.getJSONArray(bundleKey).toList()
                .forEach(
                    directoryPath -> syncBundle.addDirectory(Path.of(directoryPath.toString()))
                );

            DataRoot.get().put(bundleKey, syncBundle);
        }

        String ignorePath = System.getProperty("user.dir") + "/src/main/resources/syncignore";
        IGNORE = tools.fileToLines(new File(ignorePath));

        FLOW_THREAD.start();
    }
}
