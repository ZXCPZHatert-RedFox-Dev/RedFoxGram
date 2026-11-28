package com.redfox.gram.media

expect object RedFoxVoiceRecorder {
    fun startRecording(): Boolean
    fun stopRecordingAndGetPath(): Pair<String, Int> // filePath, durationSec
    fun cancelRecording()
}
