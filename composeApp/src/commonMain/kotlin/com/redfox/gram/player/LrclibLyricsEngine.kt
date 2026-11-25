package com.redfox.gram.player

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LrclibLyricsEngine {
    private val memoryCache = mutableMapOf<String, List<LyricLine>>()

    suspend fun fetchLyricsForAnyTrack(artist: String, title: String, durationSec: Int = 180): List<LyricLine> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${artist.trim().lowercase()}|${title.trim().lowercase()}"
            memoryCache[cacheKey]?.let { return@withContext it }

            try {
                val qArtist = URLEncoder.encode(artist.trim(), "UTF-8")
                val qTitle = URLEncoder.encode(title.trim(), "UTF-8")
                val apiUrl = "https://lrclib.net"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "RedFoxGram/0.1 (Cross-Platform Material 3 Expressive)")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val synced = extractJsonField(body, "syncedLyrics")
                    if (!synced.isNullOrBlank()) {
                        val parsed = parseLrc(synced)
                        if (parsed.isNotEmpty()) {
                            memoryCache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    }
                    val plain = extractJsonField(body, "plainLyrics")
                    if (!plain.isNullOrBlank()) {
                        val lines = plain.lines().filter { it.isNotBlank() }
                        val stepMs = ((durationSec.coerceAtLeast(30) * 1000L) / lines.size.coerceAtLeast(1))
                        val timed = lines.mapIndexed { index, text -> LyricLine(index * stepMs, text.trim()) }
                        memoryCache[cacheKey] = timed
                        return@withContext timed
                    }
                }
            } catch (_: Exception) {
            }

            listOf(LyricLine(0L, "$artist — $title"))
        }

    fun parseLrc(lrcRaw: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        return lrcRaw.lines().mapNotNull { line ->
            val match = regex.find(line.trim()) ?: return@mapNotNull null
            val min = match.groupValues.toLongOrNull() ?: 0L
            val sec = match.groupValues.toLongOrNull() ?: 0L
            val fracStr = match.groupValues
            val ms = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else (fracStr.toLongOrNull() ?: 0L)
            val text = match.groupValues.trim()
            if (text.isBlank()) null else LyricLine(min * 60_000L + sec * 1000L + ms, text)
        }.sortedBy { it.timeMs }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val marker = "\"$field\":\""
        val start = json.indexOf(marker)
        if (start == -1) return null
        val contentStart = start + marker.length
        val sb = StringBuilder()
        var i = contentStart
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                when (json[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> {}
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(json[i + 1])
                }
                i += 2
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
