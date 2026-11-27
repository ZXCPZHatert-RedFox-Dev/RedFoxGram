package com.redfox.gram.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ClientPreset(val title: String, val badge: String, val description: String) {
    ULTIMATE("RedFox Ultimate", "ALL-IN-ONE", "Все функции Opex, Ayu, extera, NekoX + M3E"),
    OPEX_AYU("Opex / Ayu Mode", "STEALTH", "Максимальный призрак, анти-удаление и история правок"),
    EXTERA_CHERRY("extera / Cherry", "MEDIA & PLUGINS", "Фото без сжатия, плагины .rfplugin и чистый UI"),
    NEKO_FORK("NekoX / Fork", "TOOLS & PROXY", "Авто-переводчик сообщений, показ ID/DC и ротация прокси")
}

data class RedFoxSettingsState(
    val activePreset: ClientPreset = ClientPreset.ULTIMATE,
    val ghostReadMessages: Boolean = true,
    val ghostReadStories: Boolean = true,
    val hideTypingStatus: Boolean = true,
    val forceOffline: Boolean = false,
    val antiDeleteMessages: Boolean = true,
    val saveEditHistory: Boolean = true,
    val disableSponsoredAds: Boolean = true,
    val sendHighResPhotos: Boolean = true,
    val pluginsEnabled: Boolean = true,
    val autoTranslateForeign: Boolean = true,
    val showUserIdsAndDc: Boolean = true,
    val smartProxyRotation: Boolean = true,
    val liveLrcLyricsBar: Boolean = true,
    val wavySeekbar: Boolean = true
)

object RedFoxConfig {
    private val _state = MutableStateFlow(RedFoxSettingsState())
    val state: StateFlow<RedFoxSettingsState> = _state.asStateFlow()

    fun update(transform: (RedFoxSettingsState) -> RedFoxSettingsState) {
        _state.update(transform)
    }

    fun applyPreset(preset: ClientPreset) {
        _state.update {
            when (preset) {
                ClientPreset.ULTIMATE -> RedFoxSettingsState(activePreset = preset)
                ClientPreset.OPEX_AYU -> it.copy(
                    activePreset = preset,
                    ghostReadMessages = true,
                    ghostReadStories = true,
                    hideTypingStatus = true,
                    antiDeleteMessages = true,
                    saveEditHistory = true
                )
                ClientPreset.EXTERA_CHERRY -> it.copy(
                    activePreset = preset,
                    ghostReadMessages = false,
                    sendHighResPhotos = true,
                    pluginsEnabled = true,
                    disableSponsoredAds = true
                )
                ClientPreset.NEKO_FORK -> it.copy(
                    activePreset = preset,
                    autoTranslateForeign = true,
                    showUserIdsAndDc = true,
                    smartProxyRotation = true
                )
            }
        }
    }
}
