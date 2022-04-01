package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class MainApp {

    final public static Thread FLOW_THREAD = new Thread(new Flow(), "flow");
    final private static Tools tools = new Tools();

    public static void main(String[] args) throws JSONException {

        String configPath = System.getProperty("user.dir") + "/src/main/resources/config.json";
        String configString = tools.fileToString(new File(configPath));
        JSONObject dataRoot = new JSONObject(configString).getJSONObject("dataRoot");
        for (String bundleKey : dataRoot.keySet()) {
            SyncBundle syncBundle = new SyncBundle(bundleKey);
            dataRoot.getJSONArray(bundleKey).toList()
                .forEach(
                    directoryPath -> syncBundle.addDirectory(directoryPath.toString())
                );

            DataRoot.get().put(bundleKey, syncBundle);
        }

        FLOW_THREAD.start();
    }
}
