package com.redfox.gram.network

object RedFoxSmartTextHelper {
    fun polishDraft(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val singleSpaced = trimmed.replace(Regex("""\s+"""), " ")
        val capitalized = singleSpaced.replaceFirstChar { it.uppercaseChar() }
        val endsWithPunct = listOf(".", "!", "?", ")", "❤️", "🔥", "😂").any { capitalized.endsWith(it) }
        return if (endsWithPunct) capitalized else "$capitalized."
    }

    fun suggestQuickReplies(incomingText: String): List<String> {
        val lower = incomingText.lowercase()
        return when {
            "привет" in lower || "ку" in lower || "хай" in lower ->
                listOf("Привет! Как твои дела? ❤️", "Приветик! Рад тебя слышать ✨", "Привет, чем занимаешься?")
            "люблю" in lower || "скучаю" in lower ->
                listOf("И я тебя очень люблю ❤️", "Безумно скучаю по тебе 💋", "Скоро увидимся, родная ❤️")
            "?" in lower ->
                listOf("Да, конечно!", "Сейчас проверю и напишу ✨", "Давай обсудим чуть позже")
            else ->
                listOf("Понял тебя 👍", "Звучит круто! 🔥", "❤️")
        }
    }
}
