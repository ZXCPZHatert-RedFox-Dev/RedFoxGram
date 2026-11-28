package com.redfox.gram.media

import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

actual object RedFoxNativeAudioEngine {
    private var clip: Clip? = null

    actual fun playFile(path: String, onCompleted: () -> Unit) {
        stop()
        runCatching {
            val stream = AudioSystem.getAudioInputStream(File(path))
            val c = AudioSystem.getClip()
            c.open(stream)
            c.start()
            clip = c
        }
    }

    actual fun togglePlayPause(): Boolean {
        val c = clip ?: return false
        return if (c.isRunning) {
            c.stop()
            false
        } else {
            c.start()
            true
        }
    }

    actual fun seekToFraction(fraction: Float) {
        val c = clip ?: return
        val len = c.microsecondLength
        if (len > 0) {
            c.microsecondPosition = (len * fraction.coerceIn(0f, 1f)).toLong()
        }
    }

    actual fun getProgressFraction(): Float {
        val c = clip ?: return 0f
        val len = c.microsecondLength
        return if (len > 0) (c.microsecondPosition.toFloat() / len.toFloat()).coerceIn(0f, 1f) else 0f
    }

    actual fun stop() {
        runCatching {
            clip?.stop()
            clip?.close()
        }
        clip = null
    }
}
