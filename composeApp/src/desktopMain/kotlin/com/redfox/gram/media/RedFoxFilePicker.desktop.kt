package com.redfox.gram.media

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePickerLauncher(onFilePicked: (filePath: String, isImage: Boolean) -> Unit): () -> Unit {
    return {
        val dialog = FileDialog(null as Frame?, "Выберите фото или файл", FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        if (dir != null && file != null) {
            val f = File(dir, file)
            val ext = f.extension.lowercase()
            val isImg = ext in listOf("jpg", "jpeg", "png", "webp", "gif")
            onFilePicked(f.absolutePath, isImg)
        }
    }
}
