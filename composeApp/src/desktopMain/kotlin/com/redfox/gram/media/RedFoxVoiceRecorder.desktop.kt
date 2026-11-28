package com.redfox.gram.media

actual object RedFoxVoiceRecorder {
    actual fun startRecording(): Boolean = false
    actual fun stopRecordingAndGetPath(): Pair<String, Int> = "" to 0
    actual fun cancelRecording() {}
}
