package com.olexyn.ensync.artifacts;

import com.olexyn.min.log.LogU;

import java.io.File;

import static com.olexyn.ensync.artifacts.Constants.EMPTY;

public class SyncFile extends File {


    private final String relativePath;
    private final SyncDirectory sDir;

    public SyncFile(SyncDirectory sDir, String absolutePath) {
        super(absolutePath);
        this.sDir = sDir;
        relativePath = this.getPath().replace(sDir.directoryPath.toString(), EMPTY);
    }

    public String getRelativePath() {
        return relativePath;
    }


    /**
     * If File exists on Disk get the TimeModified from there.
     * Else try to read it from Record.
     * Else return 0 ( = oldest possible time - a value of 0 can be seen as equal to "never existed").
     * EXAMPLES:
     * If a File was deleted, then the time will be taken from statefile.
     * If a File never existed, it will have time = 0, and thus will always be overwritten.
     */
    @Override
    public long lastModified(){
        if (exists()) { return super.lastModified(); }
        LogU.infoPlain("Did not find File for " + this);
        LogU.infoPlain("Returning -1 (never existed).");
        return -1;
    }


    public  SyncFile otherFile(SyncDirectory otherSd) {
        return new SyncFile(otherSd, otherSd.directoryPath + this.relativePath);
    }

}
