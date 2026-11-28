package com.redfox.gram.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
actual fun RedFoxLocalImage(
    filePath: String,
    contentDescription: String?,
    modifier: Modifier
) {
    val bitmap = remember(filePath) {
        runCatching {
            val f = File(filePath)
            if (f.exists() && f.length() > 0) BitmapFactory.decodeFile(filePath)?.asImageBitmap() else null
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
