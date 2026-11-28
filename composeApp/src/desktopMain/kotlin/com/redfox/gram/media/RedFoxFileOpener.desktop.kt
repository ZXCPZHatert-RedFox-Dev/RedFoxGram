package com.redfox.gram.media

import java.awt.Desktop
import java.io.File

actual fun openLocalFileWithSystem(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (file.exists() && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    }
}
