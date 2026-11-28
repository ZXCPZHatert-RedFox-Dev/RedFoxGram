package com.redfox.gram.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import java.net.URLEncoder

data class ActiveTrackState(
    val isActive: Boolean = false,
    val title: String = "",
    val artist: String = "Telegram Audio",
    val filePath: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0.25f,
    val currentLyricLine: String = "Поиск синхронного текста LRCLIB...",
    val fullLyricsLines: List<String> = emptyList()
)

object RedFoxAudioController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _track = MutableStateFlow(ActiveTrackState())
    val track = _track.asStateFlow()

    fun playTrack(title: String, artist: String, filePath: String) {
        val cleanTitle = title.substringBeforeLast(".").ifBlank { "Аудиозапись" }
        _track.value = ActiveTrackState(
            isActive = true,
            title = cleanTitle,
            artist = artist.ifBlank { "RedFox Audio" },
            filePath = filePath,
            isPlaying = true,
            progress = 0.15f,
            currentLyricLine = "Загрузка текста LRCLIB..."
        )
        fetchLyricsFromLrclib(cleanTitle, artist)
    }

    fun togglePlayPause() {
        _track.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun closePlayer() {
        _track.value = ActiveTrackState(isActive = false)
    }

    private fun fetchLyricsFromLrclib(title: String, artist: String) {
        scope.launch {
            val result = runCatching {
                val q = URLEncoder.encode("$artist $title".trim(), "UTF-8")
                val json = URL("https://lrclib.net").readText()
                val syncedMatch = Regex("\"syncedLyrics\"\\s*:\\s*\"(.*?)\"").find(json)
                val plainMatch = Regex("\"plainLyrics\"\\s*:\\s*\"(.*?)\"").find(json)
                val raw = (syncedMatch?.groupValues?.getOrNull(1) ?: plainMatch?.groupValues?.getOrNull(1) ?: "")
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                raw.lines()
                    .map { it.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]"), "").trim() }
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
                        currentLyricLine = "♪ $title — воспроизведение в M3E",
                        fullLyricsLines = listOf("Текст песни не найден в базе LRCLIB для этого трека")
                    )
                }
            }
        }
    }
}
