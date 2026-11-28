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
        val parts = cleanTitle.split("—", "–", " - ").map { it.trim() }.filter { it.isNotBlank() }
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

    private fun queryUrl(urlStr: String, extraHeaders: Map<String, String> = emptyMap()): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; RedFoxGram) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
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

    private fun extractAllJsonStringFields(json: String, field: String): List<String> {
        val results = mutableListOf<String>()
        val marker = "\"$field\":"
        var searchFrom = 0
        while (true) {
            val startIdx = json.indexOf(marker, searchFrom)
            if (startIdx == -1) break
            var cursor = startIdx + marker.length
            while (cursor < json.length && json.get(cursor).isWhitespace()) cursor++
            if (cursor < json.length && json.get(cursor) == '"') {
                val sb = StringBuilder()
                var i = cursor + 1
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
                val text = sb.toString().trim()
                if (text.isNotEmpty()) results.add(text)
                searchFrom = i + 1
            } else {
                searchFrom = cursor + 1
            }
        }
        return results
    }

    private fun cleanMetadataTerm(raw: String): String {
        return raw
            .replace(Regex("""\(.*?\)"""), "")
            .replace(Regex("""\[.*?\]"""), "")
            .replace("группа", "", ignoreCase = true)
            .trim()
    }

    private fun fetchFromMusixmatch(title: String, artist: String): List<LrcTimedLine> {
        val encTitle = URLEncoder.encode(title, "UTF-8")
        val encArtist = URLEncoder.encode(artist, "UTF-8")
        val headers = mapOf("Cookie" to "x-mxm-user-id=undefined; x-mxm-token-guid=cd25624c-2593-4210-811d-3c6678921234")
        val urls = listOf(
            "https://musixmatch.com",
            "https://musixmatch.com"
        )
        for (u in urls) {
            val json = runCatching { queryUrl(u, headers) }.getOrDefault("")
            if (json.isBlank()) continue
            val subBody = extractAllJsonStringFields(json, "subtitle_body").firstOrNull() ?: ""
            val lyrBody = extractAllJsonStringFields(json, "lyrics_body").firstOrNull() ?: ""
            val lines = parseLrcLines(subBody, lyrBody)
            if (lines.isNotEmpty()) return lines
        }
        return emptyList()
    }

    private fun fetchFromNetEase(title: String, artist: String): List<LrcTimedLine> {
        val q = URLEncoder.encode("$artist $title".trim(), "UTF-8")
        val searchJson = runCatching {
            queryUrl("https://163.com")
        }.getOrDefault("")
        val idMatch = Regex(""""id":(\d{5,12})""").find(searchJson) ?: return emptyList()
        val songId = idMatch.groupValues.getOrNull(1) ?: return emptyList()
        val lrcJson = runCatching {
            queryUrl("https://163.com")
        }.getOrDefault("")
        val rawLrc = extractAllJsonStringFields(lrcJson, "lyric").firstOrNull() ?: ""
        return parseLrcLines(rawLrc, rawLrc)
    }

    private fun fetchLyricsFromLrclib(title: String, artist: String) {
        scope.launch {
            val cleanTitle = cleanMetadataTerm(title)
            val cleanArtist = cleanMetadataTerm(artist)

            val candidateUrls = buildList {
                val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
                val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                if (cleanArtist.isNotBlank() && cleanArtist != "RedFox Audio" && cleanArtist != "Telegram") {
                    add("https://lrclib.net")
                    val encBoth = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
                    add("https://lrclib.net")
                }
                add("https://lrclib.net")
                add("https://lrclib.net")
                val altYo = cleanTitle.replace('ё', 'е').replace('Ё', 'Е')
                if (altYo != cleanTitle) {
                    val eAlt = URLEncoder.encode(altYo, "UTF-8")
                    add("https://lrclib.net")
                    add("https://lrclib.net")
                }
                val words = cleanTitle.split(" ").filter { it.length >= 4 }
                if (words.isNotEmpty()) {
                    val keyWord = URLEncoder.encode(words.last(), "UTF-8")
                    add("https://lrclib.net")
                    if (cleanArtist.isNotBlank() && cleanArtist != "RedFox Audio") {
                        val artAndWord = URLEncoder.encode("$cleanArtist ${words.last()}", "UTF-8")
                        add("https://lrclib.net")
                    }
                }
            }

            var foundLines: List<LrcTimedLine> = emptyList()
            // 1. База LRCLIB
            for (url in candidateUrls) {
                val json = runCatching { queryUrl(url) }.getOrDefault("")
                if (json.isBlank() || json == "[]") continue

                val allSynced = extractAllJsonStringFields(json, "syncedLyrics")
                val bestSynced = allSynced.firstOrNull { it.contains("[") } ?: ""
                val allPlain = extractAllJsonStringFields(json, "plainLyrics")
                val bestPlain = allPlain.firstOrNull { it.length > 20 } ?: ""

                val parsed = parseLrcLines(bestSynced, bestPlain)
                if (parsed.isNotEmpty()) {
                    foundLines = parsed
                    break
                }
            }

            // 2. База Musixmatch
            if (foundLines.isEmpty()) {
                foundLines = fetchFromMusixmatch(cleanTitle, cleanArtist)
            }

            // 3. База NetEase Cloud Music
            if (foundLines.isEmpty()) {
                foundLines = fetchFromNetEase(cleanTitle, cleanArtist)
            }

            // 4. База Lyrics.ovh
            if (foundLines.isEmpty() && cleanArtist.isNotBlank() && cleanArtist != "RedFox Audio") {
                val ovhJson = runCatching {
                    val a = URLEncoder.encode(cleanArtist, "UTF-8").replace("+", "%20")
                    val t = URLEncoder.encode(cleanTitle, "UTF-8").replace("+", "%20")
                    queryUrl("https://lyrics.ovh")
                }.getOrDefault("")
                val plainOvh = extractAllJsonStringFields(ovhJson, "lyrics").firstOrNull() ?: ""
                if (plainOvh.isNotBlank()) {
                    foundLines = parseLrcLines("", plainOvh)
                }
            }

            _track.update { cur ->
                if (foundLines.isNotEmpty()) {
                    cur.copy(
                        currentLyricLine = foundLines.first().text,
                        timedLyrics = foundLines
                    )

} else {
cur.copy(
currentLyricLine = "$cleanArtist — $cleanTitle",
timedLyrics = listOf(
LrcTimedLine(0L, "$cleanArtist — $cleanTitle"),
LrcTimedLine(3000L, "Текст не найден в LRCLIB, Musixmatch и NetEase")
)
)
}
}
}
}
}
