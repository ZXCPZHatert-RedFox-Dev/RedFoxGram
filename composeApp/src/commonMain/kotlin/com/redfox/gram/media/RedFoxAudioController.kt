package com.redfox.gram.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URL
import java.net.URLEncoder

data class ActiveTrackState(
    val isActive: Boolean = false,
    val title: String = "",
    val artist: String = "Telegram Audio",
    val filePath: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0.05f,
    val currentLyricLine: String = "Поиск синхронного текста LRCLIB...",
    val fullLyricsLines: List<String> = emptyList()
)

object RedFoxAudioController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _track = MutableStateFlow(ActiveTrackState())
    val track = _track.asStateFlow()
    private var progressJob: Job? = null

    fun playTrack(title: String, artist: String, filePath: String) {
        val cleanTitle = title.substringBeforeLast(".").ifBlank { "Аудиозапись" }
        val parts = cleanTitle.split("—", "-").map { it.trim() }
        val parsedArtist = if (parts.size >= 2) parts.component1() else artist.ifBlank { "RedFox Audio" }
        val parsedTitle = if (parts.size >= 2) parts.component2() else cleanTitle

        RedFoxNativeAudioEngine.playFile(filePath) {
            _track.update { it.copy(isPlaying = false, progress = 0f) }
        }

        _track.value = ActiveTrackState(
            isActive = true,
            title = parsedTitle,
            artist = parsedArtist,
            filePath = filePath,
            isPlaying = true,
            progress = 0.02f,
            currentLyricLine = "Загрузка текста LRCLIB..."
        )
        startProgressTicker()
        fetchLyricsFromLrclib(parsedTitle, parsedArtist)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _track.value.isActive) {
                delay(250L)
                if (_track.value.isPlaying) {
                    val p = RedFoxNativeAudioEngine.getProgressFraction()
                    val lines = _track.value.fullLyricsLines
                    val activeLine = if (lines.isNotEmpty()) {
                        val idx = (p * lines.size).toInt().coerceIn(0, lines.lastIndex)
                        "♪ ${lines.get(idx)}"
                    } else {
                        _track.value.currentLyricLine
                    }
                    _track.update { it.copy(progress = p, currentLyricLine = activeLine) }
                }
            }
        }
    }

    fun togglePlayPause() {
        val nowPlaying = RedFoxNativeAudioEngine.togglePlayPause()
        _track.update { it.copy(isPlaying = nowPlaying) }
    }

    fun seekTo(fraction: Float) {
        RedFoxNativeAudioEngine.seekToFraction(fraction)
        _track.update { it.copy(progress = fraction.coerceIn(0f, 1f)) }
    }

    fun closePlayer() {
        progressJob?.cancel()
        RedFoxNativeAudioEngine.stop()
        _track.value = ActiveTrackState(isActive = false)
    }

    private fun fetchLyricsFromLrclib(title: String, artist: String) {
        scope.launch {
            val result = runCatching {
                val q = URLEncoder.encode("$artist $title".trim(), "UTF-8")
                val json = URL("https://lrclib.net").readText()
                val syncedRegex = Regex("""\"syncedLyrics\"\s*:\s*\"(.*?)\"""")
                val plainRegex = Regex("""\"plainLyrics\"\s*:\s*\"(.*?)\"""")
                val timeTagRegex = Regex("""\[\d{2}:\d{2}\.\d{2,3}\]""")

                val raw = (syncedRegex.find(json)?.groupValues?.getOrNull(1)
                    ?: plainRegex.find(json)?.groupValues?.getOrNull(1)
                    ?: "")
                    .replace("\\n", "\n")
                    .replace("\\r", "")

                raw.lines()
                    .map { it.replace(timeTagRegex, "").trim() }
                    .filter { it.isNotBlank() }
            }.getOrDefault(emptyList())

            _track.update { cur ->
                if (result.isNotEmpty()) {
                    cur.copy(
                        currentLyricLine = "♪ ${result.first()}",
                        fullLyricsLines = result
                    )
                } else {
                    cur.copy(
                        currentLyricLine = "♪ $artist — $title",
                        fullLyricsLines = listOf("Текст песни не найден в базе LRCLIB для этого трека")
                    )
                }
            }
        }
    }
}
