package com.redfox.gram.media

import android.media.MediaRecorder
import java.io.File

actual object RedFoxVoiceRecorder {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    actual fun startRecording(): Boolean {
        cancelRecording()
        return runCatching {
            val out = File(System.getProperty("java.io.tmpdir"), "voice_${System.currentTimeMillis()}.ogg")
            val mr = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setAudioSamplingRate(48000)
                setAudioEncodingBitRate(32000)
                setOutputFile(out.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            currentFile = out
            startTimeMs = System.currentTimeMillis()
            true
        }.getOrDefault(false)
    }

    actual fun stopRecordingAndGetPath(): Pair<String, Int> {
        val durSec = (((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()).coerceAtLeast(1)
        val path = currentFile?.absolutePath ?: ""
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        return path to durSec
    }

    actual fun cancelRecording() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        runCatching { currentFile?.delete() }
        currentFile = null
    }
}
