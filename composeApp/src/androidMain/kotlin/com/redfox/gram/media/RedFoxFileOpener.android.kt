package com.redfox.gram.media

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.webkit.MimeTypeMap
import java.io.File

actual fun openLocalFileWithSystem(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return
        val activityThread = Class.forName("android.app.ActivityThread")
        val app = activityThread.getMethod("currentApplication").invoke(null) as? Application ?: return

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())

        val ext = file.extension.lowercase()
        val mime = when (ext) {
            "apk" -> "application/vnd.android.package-archive"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        app.startActivity(intent)
    }
}
