package com.redfox.gram.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LrcTimedLine(
    val timeMs: Long,
    val text: String
)

data class ActiveTrackState(
    val isActive: Boolean = false,
    val title: String = "",
    val artist: String = "RedFox Audio",
    val filePath: String = "",
    val albumArt: ImageBitmap? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentLyricLine: String = "Поиск текста LRCLIB...",
    val activeLineIndex: Int = 0,
    val lineFillProgress: Float = 0f,
    val timedLyrics: List<LrcTimedLine> = emptyList()
)

object RedFoxAudioController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _track = MutableStateFlow(ActiveTrackState())
    val track = _track.asStateFlow()
    private var progressJob: Job? = null

    fun playTrack(title: String, artist: String, filePath: String) {
        val cleanTitle = title.substringBeforeLast(".").replace("🎵", "").trim().ifBlank { "Аудиозапись" }
        val parts = cleanTitle.split("—", "–", "-").map { it.trim() }.filter { it.isNotBlank() }
        val parsedArtist = if (parts.size >= 2) parts.get(0) else artist.ifBlank { "RedFox Audio" }
        val parsedTitle = if (parts.size >= 2) parts.get(1) else cleanTitle
        val art = RedFoxNativeAudioEngine.extractAlbumArt(filePath)

        RedFoxNativeAudioEngine.playFile(filePath) {
            _track.update { it.copy(isPlaying = false, progress = 0f, currentMs = 0L) }
        }

        _track.value = ActiveTrackState(
            isActive = true,
            title = parsedTitle,
            artist = parsedArtist,
            filePath = filePath,
            albumArt = art,
            isPlaying = true,
            progress = 0f,
            currentMs = 0L,
            durationMs = RedFoxNativeAudioEngine.getDurationMs(),
            currentLyricLine = "Загрузка караоке LRCLIB..."
        )
        startProgressTicker()
        fetchLyricsFromLrclib(parsedTitle, parsedArtist)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _track.value.isActive) {
                delay(120L)
                if (_track.value.isPlaying) {
                    val posMs = RedFoxNativeAudioEngine.getCurrentPositionMs()
                    val durMs = RedFoxNativeAudioEngine.getDurationMs().coerceAtLeast(1L)
                    val p = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                    val lines = _track.value.timedLyrics

                    if (lines.isNotEmpty()) {
                        var idx = lines.indexOfLast { it.timeMs <= posMs }
                        if (idx < 0) idx = 0
                        val startT = lines.get(idx).timeMs
                        val endT = if (idx + 1 < lines.size) lines.get(idx + 1).timeMs else durMs
                        val span = (endT - startT).coerceAtLeast(400L)
                        val fill = ((posMs - startT).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                        _track.update {
                            it.copy(
                                progress = p,
                                currentMs = posMs,
                                durationMs = durMs,
                                activeLineIndex = idx,
                                lineFillProgress = fill,
                                currentLyricLine = lines.get(idx).text
                            )
                        }
                    } else {
                        _track.update {
                            it.copy(
                                progress = p,
                                currentMs = posMs,
                                durationMs = durMs
                            )
                        }
                    }
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
        val dur = _track.value.durationMs
        _track.update {
            it.copy(
                progress = fraction.coerceIn(0f, 1f),
                currentMs = (dur * fraction.coerceIn(0f, 1f)).toLong()
            )
        }
    }

    fun closePlayer() {
        progressJob?.cancel()
        RedFoxNativeAudioEngine.stop()
        _track.value = ActiveTrackState(isActive = false)
    }

    private fun queryLrclibUrl(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "RedFoxGram/1.0 (https://github.com)")
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        return conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    }

    private fun parseLrcLines(rawSynced: String, rawPlain: String): List<LrcTimedLine> {
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")
        if (rawSynced.isNotBlank()) {
            val parsed = mutableListOf<LrcTimedLine>()
            rawSynced.lines().forEach { line ->
                val m = timeRegex.find(line)
                if (m != null) {
                    val min = m.groupValues.getOrNull(1)?.toLongOrNull() ?: 0L
                    val sec = m.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
                    val fracStr = m.groupValues.getOrNull(3) ?: "0"
                    val msPart = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else (fracStr.toLongOrNull() ?: 0L)
                    val totalMs = min * 60_000L + sec * 1000L + msPart
                    val cleanText = line.replace(timeRegex, "").trim()
                    if (cleanText.isNotBlank()) {
                        parsed.add(LrcTimedLine(totalMs, cleanText))
                    }
                }
            }
            if (parsed.isNotEmpty()) return parsed.sortedBy { it.timeMs }
        }
        if (rawPlain.isNotBlank()) {
            val plainLines = rawPlain.lines().map { it.trim() }.filter { it.isNotBlank() }
            return plainLines.mapIndexed { i, txt -> LrcTimedLine(i * 3500L, txt) }
        }
        return emptyList()
    }

    private fun extractJsonStringField(json: String, field: String): String {
        val marker = "\"$field\":"
        val startIdx = json.indexOf(marker)
        if (startIdx == -1) return ""
        val firstQuote = json.indexOf('"', startIdx + marker.length)
        if (firstQuote == -1) return ""
        val sb = StringBuilder()
        var i = firstQuote + 1
        var escaped = false
        while (i < json.length) {
            val ch = json.get(i)
            if (escaped) {
                when (ch) {
                    'n' -> sb.append('\n')
                    'r' -> {}
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(ch)
                }
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == '"') {
                break
            } else {
                sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    private fun fetchLyricsFromLrclib(title: String, artist: String) {
        scope.launch {
            val parsedLines = runCatching {
                val encTitle = URLEncoder.encode(title, "UTF-8")
                val encArtist = URLEncoder.encode(artist, "UTF-8")
                var json = runCatching {
                    queryLrclibUrl("https://lrclib.net")
                }.getOrDefault("")
                if (!json.contains("syncedLyrics") && !json.contains("plainLyrics")) {
                    val encQ = URLEncoder.encode("$artist $title".trim(), "UTF-8")
                    json = queryLrclibUrl("https://lrclib.net")
                }
                val synced = extractJsonStringField(json, "syncedLyrics")
                val plain = extractJsonStringField(json, "plainLyrics")
                parseLrcLines(synced, plain)
            }.getOrDefault(emptyList())

            _track.update { cur ->
                if (parsedLines.isNotEmpty()) {
                    cur.copy(
                        currentLyricLine = parsedLines.first().text,
                        timedLyrics = parsedLines
                    )
                } else {
                    cur.copy(
                        currentLyricLine = "$artist — $title",
                        timedLyrics = listOf(
                            LrcTimedLine(0L, "$artist — $title"),
                            LrcTimedLine(3000L, "Текст песни не найден в базе LRCLIB")
                        )
                    )
                }
            }
        }
    }
}
