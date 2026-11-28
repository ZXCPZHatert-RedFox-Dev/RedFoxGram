package com.redfox.gram.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SavedBookmarkItem(
    val id: Long,
    val chatTitle: String,
    val text: String,
    val tag: String,
    val createdAtMs: Long
)

object RedFoxBookmarksStore {
    private val _items = MutableStateFlow<List<SavedBookmarkItem>>(emptyList())
    val items = _items.asStateFlow()

    fun addBookmark(chatTitle: String, text: String, tag: String) {
        if (text.isBlank()) return
        val newItem = SavedBookmarkItem(
            id = System.currentTimeMillis(),
            chatTitle = chatTitle,
            text = text,
            tag = tag,
            createdAtMs = System.currentTimeMillis()
        )
        _items.update { listOf(newItem) + it }
    }

    fun removeBookmark(id: Long) {
        _items.update { list -> list.filterNot { it.id == id } }
    }
}
