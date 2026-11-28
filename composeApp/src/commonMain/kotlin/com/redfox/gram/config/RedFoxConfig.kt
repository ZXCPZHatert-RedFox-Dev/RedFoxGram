package com.redfox.gram.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ClientPreset(val title: String, val badge: String, val description: String) {
    ULTIMATE("RedFox Ultimate", "ALL-IN-ONE M3E", "Все фичи Opexgram, AyuGram, Cherrygram, Hortay, MonoGram, extera + M3E"),
    OPEX_FORTRESS("Opexgram Fortress", "OPEX STEALTH", "Локальный пароль-маскировка, инспектор DC/ID, анти-скриншот и фильтр спама"),
    AYU_STEALTH("AyuGram Stealth", "GHOST & HISTORY", "Максимальный призрак, анти-удаление и локальная история правок"),
    CHERRY_EXTERA("Cherrygram & extera", "CHATS & MEDIA", "Пересылка без автора, перемотка кружочков, плагины .rfplugin"),
    HORTAY_MONO("Hortay & MonoGram", "PURE M3E FEED", "Лента каналов в стиле M3E, пружинная физика и дизайн Material 3"),
    MATE_MATERIAL("Mategram & Materialgram", "TOOLS & ADMIN", "Автоперевод, быстрое меню админа, массовая очистка >100 сообщ."),
    NEKO_FORK("NekoX Fork", "PROXY & ID", "Умная ротация прокси, показ ID/DC и расширенные настройки")
}

data class RedFoxSettingsState(
    val activePreset: ClientPreset = ClientPreset.ULTIMATE,
    // --- Opexgram (Безопасность, инспектор, фильтры) ---
    val opexFakePasscodeMask: Boolean = true,
    val opexMessageInspector: Boolean = true,
    val opexKeywordSpamFilter: Boolean = true,
    val opexAntiScreenshotAlert: Boolean = true,
    val opexShowClientDeviceIcon: Boolean = true,
    val opexQuickSaveToFavorites: Boolean = true,
    // --- AyuGram (Стелс и история) ---
    val ghostReadMessages: Boolean = true,
    val ghostReadStories: Boolean = true,
    val hideTypingStatus: Boolean = true,
    val forceOffline: Boolean = false,
    val antiDeleteMessages: Boolean = true,
    val saveEditHistory: Boolean = true,
    // --- Cherrygram & exteraGram (Удобство и медиа) ---
    val quickForwardNoQuote: Boolean = true,
    val seekableVideoNotes: Boolean = true,
    val sendHighResPhotos: Boolean = true,
    val pluginsEnabled: Boolean = true,
    val disableSponsoredAds: Boolean = true,
    val hidePhoneNumber: Boolean = true,
    // --- Hortay, MonoGram & Materialgram (M3E и лента) ---
    val channelFeedMode: Boolean = false,
    val springPhysicsScroll: Boolean = true,
    val googleSansFont: Boolean = true,
    val adminQuickMenu: Boolean = true,
    // --- Mategram, NekoX & RedFox (Перевод, прокси, плеер) ---
    val autoTranslateForeign: Boolean = true,
    val showUserIdsAndDc: Boolean = true,
    val smartProxyRotation: Boolean = true,
    val liveLrcLyricsBar: Boolean = true,
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "0000",
    val hiddenChatIds: Set<Long> = emptySet(),
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
                ClientPreset.OPEX_FORTRESS -> it.copy(
                    activePreset = preset,
                    opexFakePasscodeMask = true,
                    opexMessageInspector = true,
                    opexKeywordSpamFilter = true,
                    opexAntiScreenshotAlert = true,
                    opexShowClientDeviceIcon = true,
                    opexQuickSaveToFavorites = true,
                    ghostReadMessages = true,
                    antiDeleteMessages = true
                )
                ClientPreset.AYU_STEALTH -> it.copy(
                    activePreset = preset,
                    ghostReadMessages = true,
                    ghostReadStories = true,
                    hideTypingStatus = true,
                    antiDeleteMessages = true,
                    saveEditHistory = true
                )
                ClientPreset.CHERRY_EXTERA -> it.copy(
                    activePreset = preset,
                    ghostReadMessages = false,
                    quickForwardNoQuote = true,
                    seekableVideoNotes = true,
                    sendHighResPhotos = true,
                    pluginsEnabled = true,
                    disableSponsoredAds = true
                )
                ClientPreset.HORTAY_MONO -> it.copy(
                    activePreset = preset,
                    channelFeedMode = true,
                    springPhysicsScroll = true,
                    googleSansFont = true,
                    wavySeekbar = true
                )
                ClientPreset.MATE_MATERIAL -> it.copy(
                    activePreset = preset,
                    autoTranslateForeign = true,
                    adminQuickMenu = true,
                    googleSansFont = true
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
