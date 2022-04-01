package com.olexyn.ensync.files;

import com.olexyn.ensync.Execute;
import com.olexyn.ensync.Flow;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class FileTest {

    final public static Thread FLOW_THREAD = new Thread(new Flow(), "flow");
    final private static Tools tools = new Tools();
    final private HashMap<String, SyncBundle> mapOfSyncMaps = DataRoot.get();

    public long fileOpsPause = 800;
    public long assertPause = 4000;


    Execute x = new Execute();


    private static final String TEST_RESOURCES = System.getProperty("user.dir") + "/src/test/resources";
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TestFile a = new TestFile(TEST_RESOURCES + "/a/testfile.txt");
    private final TestFile b = new TestFile(TEST_RESOURCES + "/b/testfile.txt");

    private List<String> createFile(File file) {
        List<String> stringList = new ArrayList<>();
        try {
            stringList.add(LocalDateTime.now().format(dateTimeFormatter) + " CREATED");
            tools.writeStringListToFile(file.getAbsolutePath(), stringList);
            Thread.sleep(fileOpsPause);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        return stringList;
    }

    private List<String> updateFile(File file) {
        List<String> stringList = new ArrayList<>();
        try {
            stringList.addAll(tools.fileToLines(file));
            stringList.add(LocalDateTime.now().format(dateTimeFormatter) + " UPDATED");
            tools.writeStringListToFile(file.getAbsolutePath(), stringList);
            Thread.sleep(fileOpsPause);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        return stringList;
    }


    private void deleteFile(File file) {
        try {
            List<String> cmd = List.of("rm", "-r", file.getAbsolutePath());
            x.execute(cmd);
            Thread.sleep(fileOpsPause);
        } catch (InterruptedException e) {
            System.out.println("");
        }
    }

    private void cleanDirs() {
        deleteFile(a);
        deleteFile(b);
    }

    /**
     * Perform the 15 test cases in TestCases.xlsx.
     * Simple means with a static test-config.json, thus no SyncMaps are added at runtime.
     */
    @Test
    public void doSimpleFileTests() throws JSONException {

        String configPath = System.getProperty("user.dir") + "/src/test/resources/test-config.json";
        String configString = tools.fileToString(new File(configPath));
        JSONObject jsonMapOfSyncMaps = new JSONObject(configString).getJSONObject("jsonMapOfSyncMaps");
//        for (String key : jsonMapOfSyncMaps.keySet()) {
//            SyncMap syncMap = new SyncMap(key);
//            for (Object jsonSyncDirPath : jsonMapOfSyncMaps.getJSONArray(key).toList()) {
//                syncMap.addDirectory(jsonSyncDirPath.toString());
//            }
//            MapOfSyncMaps.get().put(key, syncMap);
//
//        }


        FLOW_THREAD.start();

        List<String> sideloadContentA;
        List<String> sideloadContentB;
        cleanDirs();

        // 1
        createFile(a);
        deleteFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(a.exists());
        Assert.assertFalse(b.exists());
        cleanDirs();
        // 2
        createFile(b);
        createFile(a);
        deleteFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(a.exists());
        Assert.assertFalse(b.exists());
        cleanDirs();
        // 3
        createFile(a);
        createFile(b);
        deleteFile(a);
        deleteFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(a.exists());
        Assert.assertFalse(b.exists());
        cleanDirs();
        // 4
        createFile(a);
        deleteFile(a);
        sideloadContentB = createFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        cleanDirs();
        // 5
        createFile(a);
        createFile(b);
        deleteFile(a);
        sideloadContentB = updateFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        cleanDirs();
        // 6
        sideloadContentA = createFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, b.updateContent().getContent());
        // 7
        createFile(b);
        createFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, b.updateContent().getContent());
        // 8
        createFile(a);
        createFile(b);
        deleteFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(a.exists());
        Assert.assertFalse(b.exists());
        cleanDirs();
        //9
        createFile(a);
        sideloadContentB = createFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        cleanDirs();
        // 10
        createFile(b);
        createFile(a);
        sideloadContentB = updateFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        // 11
        createFile(a);
        sideloadContentA = updateFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, b.updateContent().getContent());
        // 12
        createFile(a);
        createFile(b);
        sideloadContentA = updateFile(a);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, b.updateContent().getContent());
        // 13
        createFile(a);
        createFile(b);
        updateFile(a);
        deleteFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(a.exists());
        Assert.assertFalse(b.exists());
        cleanDirs();
        // 14
        createFile(a);
        updateFile(a);
        sideloadContentB = createFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        cleanDirs();
        // 15
        createFile(a);
        createFile(b);
        updateFile(a);
        sideloadContentB = updateFile(b);
        try {
            Thread.sleep(assertPause);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, a.updateContent().getContent());
        cleanDirs();
    }

}
