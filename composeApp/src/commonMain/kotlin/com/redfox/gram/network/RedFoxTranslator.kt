package com.redfox.gram.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

object RedFoxTranslator {
    suspend fun translateToRussian(text: String): String = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext ""
        runCatching {
            val q = URLEncoder.encode(clean, "UTF-8")
            val url = "https://googleapis.com"
            val raw = URL(url).readText()
            // Собираем переведённые сегменты из JSON-массива
            val segments = Regex("\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"(?:[^\"\\\\]|\\\\.)*\"").findAll(raw)
                .map {
                    it.groupValues.getOrNull(1)
                        ?.replace("\\n", "\n")
                        ?.replace("\\\"", "\"")
                        ?: ""
                }
                .filter { it.isNotBlank() }
                .toList()
            if (segments.isNotEmpty()) segments.joinToString("") else "Не удалось перевести текст"
        }.getOrDefault("Ошибка сети при переводе")
    }
}
