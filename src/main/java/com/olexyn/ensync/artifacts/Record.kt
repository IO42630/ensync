package com.olexyn.ensync.artifacts

import java.io.File
import java.nio.file.Path

class Record(val targetPath: Path) {

    var files: Map<String, RecordFile> = HashMap()

    fun getPath(): Path {
        return targetPath.resolve(Constants.STATE_FILE_NAME)
    }

    private fun getFile(): File {
        return getPath().toFile();
    }

    fun exists(): Boolean {
        return getFile().exists()
    }

    fun lastModified(key: String): Long {
        val record: RecordFile? = files.get(key);
        if (record == null) { return -1 }
        return record.timeModifiedFromRecord
    }

}