package com.olexyn.ensync.files;

import com.olexyn.ensync.LogUtil;
import com.olexyn.ensync.Tools;
import com.olexyn.ensync.artifacts.SyncDirectory;
import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.ensync.lock.LockUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TestFile extends File {

    private static final Logger LOGGER = LogUtil.get(TestFile.class);

    Tools tools = new Tools();

    /**
     * Wrapper for File that adds tools for assessing it's state.
     */
    public TestFile(String pathname) {
        super(pathname);
    }

    public List<String> readContent() {
        LOGGER.info("TEST TRY READ: " + toPath());
        var fcState = LockUtil.lockFile(toPath());
        return tools.fileToLines(fcState.getFc());
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        TestFile that = (TestFile) o;





        return Objects.equals(tools, that.tools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tools);
    }
}
