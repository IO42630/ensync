package com.olexyn.ensync.artifacts;

import java.io.File;

public class SyncFile extends File {


    // Very IMPORTANT field. Allows to store lastModified as it is stored in the StateFile.
    public long timeModifiedFromStateFile = 0;

    public String relativePath;
    private final SyncDirectory sd;

    public SyncFile(SyncDirectory sd, String pathname) {

        super(pathname);
        this.sd = sd;
        relativePath = this.getPath().replace(sd.directoryPath, "");
    }

    public void setTimeModifiedFromStateFile(long value) {
        timeModifiedFromStateFile = value;
    }

    public long getTimeModifiedFromStateFile() {
        SyncFile record = sd.readStateFile().get(this.getPath());


        return record == null ? 0 : record.timeModifiedFromStateFile;
    }

    /**
     * If File exists on Disk get the TimeModified from there.
     * Else try to read it from StateFile.
     * Else return 0 ( = oldest possible time - a value of 0 can be seen as equal to "never existed").
     * EXAMPLES:
     * If a File was deleted, then the time will be taken from statefile.
     * If a File never existed, it will have time = 0, and thus will always be overwritten.
     */
    public long getTimeModified(){
        if (exists()) {
            return lastModified();
        }

        if (sd.readStateFile().get(getPath()) != null) {
            return getTimeModifiedFromStateFile();
        }
        return 0;
    }

    public boolean isNewer(SyncFile otherFile) {
        return this.getTimeModified() >= otherFile.getTimeModified();
    }

    public boolean isOlder(SyncFile otherFile) {
        return !isNewer(otherFile);
    }

    public  SyncFile otherFile(SyncDirectory otherSd) {
        return new SyncFile(otherSd, otherSd.directoryPath + this.relativePath);
    }

}
