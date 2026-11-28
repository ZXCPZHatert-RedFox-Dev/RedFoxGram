package com.redfox.gram.media

import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual object RedFoxNativeAudioEngine {
    private var player: MediaPlayer? = null

    actual fun playFile(path: String, onCompleted: () -> Unit) {
        stop()
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)
                setOnCompletionListener { onCompleted() }
                prepare()
                start()
            }
            player = mp
        }
    }

    actual fun extractAlbumArt(path: String): ImageBitmap? {
        return runCatching {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val bytes = mmr.embeddedPicture
            mmr.release()
            if (bytes != null && bytes.isNotEmpty()) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } else null
        }.getOrNull()
    }

    actual fun togglePlayPause(): Boolean {
        val mp = player ?: return false
        return runCatching {
            if (mp.isPlaying) {
                mp.pause()
                false
            } else {
                mp.start()
                true
            }
        }.getOrDefault(false)
    }

    actual fun seekToFraction(fraction: Float) {
        val mp = player ?: return
        runCatching {
            val dur = mp.duration
            if (dur > 0) mp.seekTo((dur * fraction.coerceIn(0f, 1f)).toInt())
        }
    }

    actual fun getProgressFraction(): Float {
        val mp = player ?: return 0f
        return runCatching {
            val dur = mp.duration
            if (dur > 0) (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }.getOrDefault(0f)
    }

    actual fun getCurrentPositionMs(): Long = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
    actual fun getDurationMs(): Long = runCatching { player?.duration?.toLong() ?: 0L }.getOrDefault(0L)

    actual fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }
}
