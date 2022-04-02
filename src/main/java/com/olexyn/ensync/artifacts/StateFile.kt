package com.olexyn.ensync.artifacts

import java.io.File

class StateFile(val targetPath: String) {

    fun getPath(): String {
        return targetPath + "/" + Constants.STATE_FILE_NAME
    }

    private fun getFile(): File {
        return File(getPath())
    }

    fun exists(): Boolean {
        return getFile().exists()
    }

}