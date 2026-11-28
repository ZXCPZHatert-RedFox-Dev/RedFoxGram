package com.redfox.gram.media

expect object RedFoxNativeAudioEngine {
    fun playFile(path: String, onCompleted: () -> Unit)
    fun togglePlayPause(): Boolean
    fun seekToFraction(fraction: Float)
    fun getProgressFraction(): Float
    fun stop()
}
