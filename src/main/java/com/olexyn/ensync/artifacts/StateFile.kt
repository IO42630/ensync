package com.olexyn.ensync.artifacts

import java.io.File
import java.nio.file.Path

class StateFile(val targetPath: Path) {

    fun getPath(): Path {
        return targetPath.resolve(Constants.STATE_FILE_NAME)
    }

    private fun getFile(): File {
        return getPath().toFile();
    }

    fun exists(): Boolean {
        return getFile().exists()
    }

}