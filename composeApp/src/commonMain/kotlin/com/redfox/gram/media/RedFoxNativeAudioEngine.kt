package com.redfox.gram.media

import androidx.compose.ui.graphics.ImageBitmap

expect object RedFoxNativeAudioEngine {
    fun playFile(path: String, onCompleted: () -> Unit)
    fun extractAlbumArt(path: String): ImageBitmap?
    fun togglePlayPause(): Boolean
    fun seekToFraction(fraction: Float)
    fun getProgressFraction(): Float
    fun getCurrentPositionMs(): Long
    fun getDurationMs(): Long
    fun stop()
}
