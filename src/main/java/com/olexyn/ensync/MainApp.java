package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;
import com.olexyn.ensync.lock.LockUtil;
import com.olexyn.ensync.util.IgnoreUtil;
import com.olexyn.min.log.LogU;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.Path;


public class MainApp {

    final public static Flow FLOW = new Flow();

    final private static Tools TOOLS = new Tools();

    public static void main(String[] args) throws JSONException {
        LogU.remake(null, "com.olexyn.ensync.", "[%1$tF %1$tT] [%2$-7s] [%3$-10s] %4$-180s [%5$s]\n");

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
        IgnoreUtil.IGNORE = Tools.fileToLines(LockUtil.lockFile(ignorePath).getFc());
        FLOW.start();
    }
}
