package com.olexyn.ensync.files;

import com.olexyn.ensync.Tools;
import com.olexyn.ensync.lock.LockUtil;
import com.olexyn.min.log.LogU;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class TestFile extends File {

    Tools tools = new Tools();

    /**
     * Wrapper for File that adds tools for assessing it's state.
     */
    public TestFile(String pathname) {
        super(pathname);
    }

    public List<String> readContent() {
        LogU.infoPlain("TEST TRY READ: " + toPath());
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
