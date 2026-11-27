package com.redfox.gram.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RedFoxSettingsState(
    val ghostReadMessages: Boolean = false,
    val ghostReadStories: Boolean = false,
    val hideTypingStatus: Boolean = false,
    val forceOffline: Boolean = false,
    val antiDeleteMessages: Boolean = true,
    val saveEditHistory: Boolean = true,
    val disableSponsoredAds: Boolean = true,
    val sendHighResPhotos: Boolean = true,
    val liveLrcLyricsBar: Boolean = true,
    val wavySeekbar: Boolean = true,
    val pluginsEnabled: Boolean = true
)

object RedFoxConfig {
    private val _state = MutableStateFlow(RedFoxSettingsState())
    val state: StateFlow<RedFoxSettingsState> = _state.asStateFlow()

    fun update(transform: (RedFoxSettingsState) -> RedFoxSettingsState) {
        _state.update(transform)
    }
}
