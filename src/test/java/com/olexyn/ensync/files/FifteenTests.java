package com.olexyn.ensync.files;

import com.olexyn.ensync.Flow;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.artifacts.DataRoot;
import com.olexyn.ensync.artifacts.SyncBundle;
import com.olexyn.ensync.lock.LockUtil;
import com.olexyn.min.log.LogU;
import org.apache.commons.io.FileUtils;
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


/**
 * Perform the 15 test cases in TestCases.xlsx.
 * Simple means with a static test-config.json, thus no SyncMaps are added at runtime.
 */
public class FifteenTests {


    final public static Flow FLOW = new Flow();


    final private static Tools tools = new Tools();

    private final static  long M1000 = 800;

    private static final Path TEMP_DIR = Path.of(System.getProperty("user.dir") + "/src/test/temp");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Path A_DIR = TEMP_DIR.resolve("AAA");
    private static final Path B_DIR = TEMP_DIR.resolve("BBB");

    private final TestFile aFile = new TestFile(A_DIR + "/testfile.txt");
    private final TestFile bFile = new TestFile(B_DIR + "/testfile.txt");

    private List<String> createFile(File file) {
        if (file.exists()) {
            LogU.infoPlain("TEST can not create existing: " + file.toPath());
            Assert.fail();
        }
        List<String> stringList = new ArrayList<>();
        stringList.add(LocalDateTime.now().format(dateTimeFormatter) + " CREATED");
        tools.writeStringListToFile(file.getAbsolutePath(), stringList);
        LogU.infoPlain("TEST CREATE: " + file.toPath());
        return stringList;
    }

    private List<String> modifyFile(File file) {
        LogU.infoPlain("TEST TRY MODIFY: " + file.toPath());
        var fcState = LockUtil.lockFile(file.toPath(), 10);
        var stringList = new ArrayList<>(tools.fileToLines(fcState.getFc()));
        stringList.add(LocalDateTime.now().format(dateTimeFormatter) + " MODIFIED");
        tools.writeStringListToFile(file.getAbsolutePath(), stringList);
        LogU.infoPlain("TEST MODIFY: " + file.toPath());
        LockUtil.unlockFile(fcState, 10);
        LogU.infoPlain("TEST MODIFY UNLOCKED: " + file.toPath());
        return stringList;
    }


    private static void deleteFile(File file) {
        LogU.infoPlain("TEST TRY DELETE: " + file.toPath());
        var fcState = LockUtil.lockFile(file.toPath(), 10);
        try {
            Files.delete(file.toPath());
            LogU.infoPlain("TEST DELETE: " + file.toPath());
        } catch (IOException e) {
            LogU.warnPlain("Could not delete file." + file.toPath());
        }
        LockUtil.unlockFile(fcState, 10);
        LogU.infoPlain("TEST DELETE UNLOCKED: " + file.toPath());
    }

    private void cleanDirs(Path... dirs) {
        for (var dir : dirs) {
            try {
                FileUtils.deleteDirectory(dir.toFile());
                Files.createDirectory(dir);
            } catch (IOException e) {
                LogU.warnPlain("Could not clear dirs. " + dir + e.getMessage());
            }
        }
    }

    SyncBundle syncBundle;


    public static void waitX(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {}
    }

    @Before
    public void prepare() {
        syncBundle = new SyncBundle("testSyncBundle");
        syncBundle.addDirectory(A_DIR);
        syncBundle.addDirectory(B_DIR);
        cleanDirs(A_DIR, B_DIR);
        DataRoot.get().put(syncBundle.name, syncBundle);
    }

    @After
    public void reset() {
        FLOW.stop();
        waitX(M1000);
        cleanDirs(A_DIR, B_DIR);
    }

    @Test
    public void test1() {
        createFile(aFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        deleteFile(aFile);
        waitX(M1000);
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void test2() {
        createFile(aFile);
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        deleteFile(aFile);
        deleteFile(bFile);
        waitX(M1000);
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void test3() {
        createFile(aFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        deleteFile(aFile);
        var bContent = createFile(bFile);
        waitX(M1000);
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void test4() {
        createFile(aFile);
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        deleteFile(aFile);
        var bContent = modifyFile(bFile);
        waitX(M1000);
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void test5() {
        FLOW.start();
        waitX(M1000);
        var aContent = createFile(aFile);
        waitX(M1000);
        Assert.assertEquals(aContent, bFile.readContent());
    }

    @Test
    public void test6() {
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        createFile(aFile);
        waitX(M1000);
        deleteFile(bFile);
        waitX(M1000);
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void test7() {
        FLOW.start();
        waitX(M1000);
        createFile(aFile);
        var bContent = createFile(bFile);
        waitX(M1000);
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void test8() {
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        createFile(aFile);
        var bContent = modifyFile(bFile);
        waitX(M1000);
        Assert.assertEquals(bContent, aFile.readContent());
    }

    @Test
    public void test9() {
        createFile(aFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        var aContent = modifyFile(aFile);
        waitX(M1000);
        Assert.assertEquals(aContent, bFile.readContent());
    }

    @Test
    public void test10() {
        createFile(aFile);
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        modifyFile(aFile);
        waitX(M1000);
        deleteFile(bFile);
        waitX(M1000);
        Assert.assertFalse(aFile.exists());
        Assert.assertFalse(bFile.exists());
    }

    @Test
    public void test12() {
        createFile(aFile);
        createFile(bFile);
        waitX(M1000);
        FLOW.start();
        waitX(M1000);
        modifyFile(aFile);
        waitX(M1000);
        var bContent = modifyFile(bFile);
        waitX(M1000);
        Assert.assertEquals(bContent, aFile.readContent());
    }

}
