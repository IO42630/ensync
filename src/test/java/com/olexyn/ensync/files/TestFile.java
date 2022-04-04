package com.olexyn.ensync.files;

import com.olexyn.ensync.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestFile extends File {

    Tools tools = new Tools();
    private List<String> content = new ArrayList<>();

    /**
     * Wrapper for File that adds tools for assessing it's state.
     */
    public TestFile(String pathname) {
        super(pathname);
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public List<String> readContent() {
        this.content = tools.fileToLines(this);
        return content;
    }

    public List<String> getContent() {
        return content;
    }

    public List<String> copyContent() {
        return List.copyOf(content);
    }

    public TestFile updateContent() {
        String line = tools.fileToLines(this).get(0);
        this.content.add(line);
        return this;
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
