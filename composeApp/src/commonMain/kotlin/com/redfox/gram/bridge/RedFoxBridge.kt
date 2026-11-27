package com.redfox.gram.bridge

import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.ui.home.ChatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.jvm.JvmStatic

data class DeletedMessageEntry(
    val dialogId: Long,
    val messageId: Int,
    val text: String,
    val authorName: String
)

object RedFoxBridge {
    private val _deletedMessages = MutableStateFlow<Map<Int, DeletedMessageEntry>>(emptyMap())
    val deletedMessages: StateFlow<Map<Int, DeletedMessageEntry>> = _deletedMessages.asStateFlow()

    private val _liveChats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val liveChats: StateFlow<List<ChatPreview>> = _liveChats.asStateFlow()

    // --- Хуки для Java-движка TMessagesProj ---

    // 1. AyuGram: Блокировка отправки галочек прочтения
    @JvmStatic
    fun shouldBlockReadReceipt(): Boolean {
        return RedFoxConfig.state.value.ghostReadMessages
    }

    // 2. AyuGram: Анонимный просмотр историй
    @JvmStatic
    fun shouldBlockStoryView(): Boolean {
        return RedFoxConfig.state.value.ghostReadStories
    }

    // 3. AyuGram: Скрытие статуса "печатает..."
    @JvmStatic
    fun shouldBlockTypingStatus(): Boolean {
        return RedFoxConfig.state.value.hideTypingStatus
    }

    // 4. AyuGram: Перехват удаленных сообщений из MessagesController.java
    @JvmStatic
    fun onMessageDeleted(dialogId: Long, messageId: Int, text: String, authorName: String) {
        if (!RedFoxConfig.state.value.antiDeleteMessages) return
        _deletedMessages.update { current ->
            current + (messageId to DeletedMessageEntry(dialogId, messageId, text, authorName))
        }
    }

    // 5. Cherrygram: Пересылка без имени отправителя
    @JvmStatic
    fun shouldForwardWithoutAuthor(): Boolean {
        return RedFoxConfig.state.value.quickForwardNoQuote
    }

    // 6. exteraGram / Materialgram: Перемотка кружочков
    @JvmStatic
    fun isSeekableVideoNotesEnabled(): Boolean {
        return RedFoxConfig.state.value.seekableVideoNotes
    }

    // 7. Передача реальных диалогов из TMessagesProj в Compose M3E
    @JvmStatic
    fun pushDialogsFromEngine(dialogs: List<ChatPreview>) {
        _liveChats.value = dialogs
    }
}
