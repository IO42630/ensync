package com.olexyn.ensync.files;

import com.olexyn.ensync.Execute;
import com.olexyn.ensync.Flow;
import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;
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


/**
 * Perform the 15 test cases in TestCases.xlsx.
 * Simple means with a static test-config.json, thus no SyncMaps are added at runtime.
 */
public class FifteenTests {

    private static final Logger LOGGER = LogUtil.get(FifteenTests.class);

    final public static Flow FLOW = new Flow();


    final private static Tools tools = new Tools();

    private final static long FILE_OPS_PAUSE = 800;
    private final static  long WAIT_BEFORE_ASSERT = 4000;


    private final static Execute x = new Execute();

    private static final Path TEMP_DIR = Path.of(System.getProperty("user.dir") + "/src/test/temp");
    private static final String RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources";
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Path A_DIR = TEMP_DIR.resolve("a");
    private static final Path B_DIR = TEMP_DIR.resolve("b");

    private final TestFile aFile = new TestFile(A_DIR + "/testfile.txt");
    private final TestFile bFile = new TestFile(B_DIR + "/testfile.txt");

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

    private static void deleteRec(Path path) {
        var file = path.toFile();
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            try {
                Files.walk(path)
                    .filter(subPath -> !subPath.equals(path))
                    .forEach(FifteenTests::deleteRec);
            } catch (IOException e) {
                LOGGER.severe("Could not walk path.");
            }
        }
        try {
            Files.delete(path);
        } catch (IOException e) {
            LOGGER.severe("Could not delete file.");
        }
    }

    private void cleanDirs(Path... dirs) {
        for (var dir : dirs) {
            deleteRec(dir);
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                LOGGER.severe("Could not clear dirs.");
            }
        }
    }

    SyncBundle syncBundle;


    public static void waitBeforeAssert() {
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT);
        } catch (InterruptedException ignored) {}
    }

    @Before
    public void prepare() {
        syncBundle = new SyncBundle("testSyncBundle");
        syncBundle.addDirectory(A_DIR);
        syncBundle.addDirectory(B_DIR);
        cleanDirs(A_DIR, B_DIR);
        DataRoot.get().put(syncBundle.name, syncBundle);
        FLOW.start();
    }

    @After
    public void reset() {
        FLOW.stop();
        cleanDirs(A_DIR, B_DIR);
    }

    @Test
    public void one() {
        createFile(aFile);
        deleteFile(aFile);
        waitBeforeAssert();
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void three() {
        createFile(aFile);
        createFile(bFile);
        deleteFile(aFile);
        deleteFile(bFile);
        waitBeforeAssert();
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void four() {
        createFile(aFile);
        deleteFile(aFile);
        var bContent = createFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void five() {
        createFile(aFile);
        createFile(bFile);
        deleteFile(aFile);
        var bContent = updateFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void six() {
        var aContent = createFile(aFile);
        waitBeforeAssert();
        Assert.assertEquals(aContent, bFile.readContent());
    }

    @Test
    public void eight() {
        createFile(aFile);
        createFile(bFile);
        deleteFile(bFile);
        waitBeforeAssert();
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void nine() {
        createFile(aFile);
        var bContent = createFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void ten() {
        createFile(bFile);
        createFile(aFile);
        var bContent = updateFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void eleven() {
        createFile(aFile);
        var aContent = updateFile(aFile);
        waitBeforeAssert();
        Assert.assertEquals(aContent, bFile.readContent());
    }

    @Test
    public void thirteen() {
        createFile(aFile);
        createFile(bFile);
        updateFile(aFile);
        deleteFile(bFile);
        waitBeforeAssert();
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void fourteen() {
        createFile(aFile);
        updateFile(aFile);
        var bContent = createFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void fifteen() {
        createFile(aFile);
        createFile(bFile);
        updateFile(aFile);
        var bContent = updateFile(bFile);
        waitBeforeAssert();
        Assert.assertEquals(bContent, aFile.readContent());
    }

}
