package com.xiamo.utils.config

import java.io.File

object ConfigManager {
    val mainDir = File("SuperSoftClient")

    init {
        if (!mainDir.exists()) mainDir.mkdir()
    }


}