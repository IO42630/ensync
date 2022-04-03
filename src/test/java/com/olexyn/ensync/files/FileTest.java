package com.olexyn.ensync.files;

import com.olexyn.ensync.Execute;
import com.olexyn.ensync.Flow;
import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.artifacts.SyncBundle;
import com.olexyn.ensync.artifacts.SyncDirectory;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class FileTest {

    private static final Logger LOGGER = LogUtil.get(FileTest.class);

    final public static Thread FLOW_THREAD = new Thread(new Flow(), "flow");
    final private static Tools tools = new Tools();

    private final static long FILE_OPS_PAUSE = 800;
    private final static  long WAIT_BEFORE_ASSERT = 4000;


    private final static Execute x = new Execute();

    private static final Path TEMP_DIR = Path.of(System.getProperty("user.dir") + "/src/test/temp");
    private static final String RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources";
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Path A_DIR = TEMP_DIR.resolve("/a");
    private static final Path B_DIR = TEMP_DIR.resolve("/b");

    private final TestFile aTestFile = new TestFile(A_DIR + "/testfile.txt");
    private final TestFile bTestFile = new TestFile(B_DIR + "/testfile.txt");

    private List<String> createFile(File file) {
        List<String> stringList = new ArrayList<>();
        try {
            stringList.add(LocalDateTime.now().format(dateTimeFormatter) + " CREATED");
            tools.writeStringListToFile(file.getAbsolutePath(), stringList);
            Thread.sleep(FILE_OPS_PAUSE);
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
            Thread.sleep(FILE_OPS_PAUSE);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        return stringList;
    }


    private static void deleteFile(File file) {
        try {
            Files.delete(file.toPath());
            Thread.sleep(FILE_OPS_PAUSE);
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Could not delete file.");
        }
    }

    private void cleanDirs(Path... dirs) {
        for (var dir : dirs) {
            try {
                Files.delete(dir);
                Files.createDirectory(dir);
            } catch (IOException e) {
                Assert.fail();
            }
        }
    }

    SyncBundle syncMap;
    List<String> sideloadContentA;
    List<String> sideloadContentB;

    @Before
    public void prepare() {
        syncMap = new SyncBundle("testSyncBundle");
        syncMap.addDirectory(A_DIR);
        syncMap.addDirectory(B_DIR);
    }

    @After
    public void reset() {

    }

    /**
     * Perform the 15 test cases in TestCases.xlsx.
     * Simple means with a static test-config.json, thus no SyncMaps are added at runtime.
     */
    @Test
    public void doSimpleFileTests() throws JSONException {


        FLOW_THREAD.start();

        List<String> sideloadContentA;
        List<String> sideloadContentB;
        cleanDirs();

        // 1
        createFile(aTestFile);
        deleteFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(aTestFile.exists());
        Assert.assertFalse(bTestFile.exists());
        cleanDirs();
        // 2
        createFile(bTestFile);
        createFile(aTestFile);
        deleteFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(aTestFile.exists());
        Assert.assertFalse(bTestFile.exists());
        cleanDirs();
        // 3
        createFile(aTestFile);
        createFile(bTestFile);
        deleteFile(aTestFile);
        deleteFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(aTestFile.exists());
        Assert.assertFalse(bTestFile.exists());
        cleanDirs();
        // 4
        createFile(aTestFile);
        deleteFile(aTestFile);
        sideloadContentB = createFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        cleanDirs();
        // 5
        createFile(aTestFile);
        createFile(bTestFile);
        deleteFile(aTestFile);
        sideloadContentB = updateFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        cleanDirs();
        // 6
        sideloadContentA = createFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, bTestFile.updateContent().getContent());
        // 7
        createFile(bTestFile);
        createFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, bTestFile.updateContent().getContent());
        // 8
        createFile(aTestFile);
        createFile(bTestFile);
        deleteFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(aTestFile.exists());
        Assert.assertFalse(bTestFile.exists());
        cleanDirs();
        //9
        createFile(aTestFile);
        sideloadContentB = createFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        cleanDirs();
        // 10
        createFile(bTestFile);
        createFile(aTestFile);
        sideloadContentB = updateFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        // 11
        createFile(aTestFile);
        sideloadContentA = updateFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, bTestFile.updateContent().getContent());
        // 12
        createFile(aTestFile);
        createFile(bTestFile);
        sideloadContentA = updateFile(aTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentA, bTestFile.updateContent().getContent());
        // 13
        createFile(aTestFile);
        createFile(bTestFile);
        updateFile(aTestFile);
        deleteFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertFalse(aTestFile.exists());
        Assert.assertFalse(bTestFile.exists());
        cleanDirs();
        // 14
        createFile(aTestFile);
        updateFile(aTestFile);
        sideloadContentB = createFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        cleanDirs();
        // 15
        createFile(aTestFile);
        createFile(bTestFile);
        updateFile(aTestFile);
        sideloadContentB = updateFile(bTestFile);
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
        Assert.assertEquals(sideloadContentB, aTestFile.updateContent().getContent());
        cleanDirs();
    }

}
