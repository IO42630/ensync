package com.olexyn.ensync.artifacts;

public class RecordFile extends SyncFile {

    // Very IMPORTANT field. Allows to store lastModified as it is stored in the Record.
    public long timeModifiedFromRecord = 0;

    public RecordFile(SyncDirectory sDir, String absolutePath) {
        super(sDir, absolutePath);
    }

    @Override
    public long lastModified() {
        return timeModifiedFromRecord;
    }

    public void setTimeModifiedFromRecord(long value) {
        timeModifiedFromRecord = value;
    }


}
