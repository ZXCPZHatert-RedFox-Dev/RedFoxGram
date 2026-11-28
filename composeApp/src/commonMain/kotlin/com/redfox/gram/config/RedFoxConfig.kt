package com.redfox.gram.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RedFoxSettingsState(
    val ghostReadMessages: Boolean = true,
    val ghostReadStories: Boolean = true,
    val hideTypingStatus: Boolean = true,
    val antiDeleteMessages: Boolean = true,
    val quickForwardNoQuote: Boolean = true,
    val seekableVideoNotes: Boolean = true,
    val springPhysicsScroll: Boolean = true,
    val liveLrcLyricsBar: Boolean = true,
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "0000",
    val hiddenChatIds: Set<Long> = emptySet(),
    val themePreset: String = "MONET",
    val bubbleCornerDp: Int = 22,
    val chatFontScale: Float = 1.0f,
    val syncedTelegramAccentHex: Long = 0L,
    val syncedThemeName: String = "",
    val antiScreenshotEnabled: Boolean = false,
    val ambientPhotoGlow: Boolean = true,
    val expressiveSliders: Boolean = true,
    val sliderTrackHeightDp: Float = 20f,
    val sliderThumbHeightDp: Float = 44f,
    val barrelScrollEffect: Boolean = true,
    val messageTails: Boolean = true,
    val glassMessages: Boolean = true,
    val headerBlurStyle: Boolean = true,
    val fontStyleName: String = "Системный",
    val avatarShapeOrdinal: Int = 0,
    val customHeaderTitle: String = "",
    val centerHeaderTitle: Boolean = true,
    val showSearchInHeader: Boolean = true,
    val holidaySnowEnabled: Boolean = false,
    val holidaySantaHatEnabled: Boolean = false,
    val navBarStyle: String = "FLOATING",
    val navShowProfileTab: Boolean = true,
    val navActionInRow: Boolean = true,
    val iconColorMode: String = "HARMONIZED",
    val folderTabStyle: String = "BUTTONS",
    val hideAllChatsFolder: Boolean = false,
    val deleteEffectStyle: String = "PORTAL",
    val inputFieldStyle: String = "EXPRESSIVE",
    val doubleTapReaction: String = "❤️",
    val showSecondsInTime: Boolean = true,
    val showScrollToTopBtn: Boolean = true,
    val animatedTypingCursor: Boolean = true
)

object RedFoxConfig {
    private val _state = MutableStateFlow(RedFoxSettingsState())
    val state: StateFlow<RedFoxSettingsState> = _state.asStateFlow()

    fun update(transform: (RedFoxSettingsState) -> RedFoxSettingsState) {
        _state.update(transform)
    }

    fun exportSettingsOpex(): String {
        val st = _state.value
        return buildString {
            appendLine("# RedFoxGram / Opexgram Backup (.opex)")
            appendLine("ghostReadMessages=${st.ghostReadMessages}")
            appendLine("hideTypingStatus=${st.hideTypingStatus}")
            appendLine("antiDeleteMessages=${st.antiDeleteMessages}")
            appendLine("liveLrcLyricsBar=${st.liveLrcLyricsBar}")
            appendLine("pinLockEnabled=${st.pinLockEnabled}")
            appendLine("pinCode=${st.pinCode}")
            appendLine("themePreset=${st.themePreset}")
            appendLine("bubbleCornerDp=${st.bubbleCornerDp}")
            appendLine("chatFontScale=${st.chatFontScale}")
            appendLine("antiScreenshotEnabled=${st.antiScreenshotEnabled}")
            appendLine("ambientPhotoGlow=${st.ambientPhotoGlow}")
        }
    }

    fun importSettingsOpex(raw: String): Boolean {
        val lines = raw.lines().map { it.trim() }.filter { it.contains("=") && !it.startsWith("#") }
        if (lines.isEmpty()) return false
        val map = lines.associate {
            val idx = it.indexOf('=')
            it.substring(0, idx).trim() to it.substring(idx + 1).trim()
        }
        update { cur ->
            cur.copy(
                ghostReadMessages = map.get("ghostReadMessages")?.toBooleanStrictOrNull() ?: cur.ghostReadMessages,
                hideTypingStatus = map.get("hideTypingStatus")?.toBooleanStrictOrNull() ?: cur.hideTypingStatus,
                antiDeleteMessages = map.get("antiDeleteMessages")?.toBooleanStrictOrNull() ?: cur.antiDeleteMessages,
                liveLrcLyricsBar = map.get("liveLrcLyricsBar")?.toBooleanStrictOrNull() ?: cur.liveLrcLyricsBar,
                pinLockEnabled = map.get("pinLockEnabled")?.toBooleanStrictOrNull() ?: cur.pinLockEnabled,
                pinCode = map.get("pinCode")?.take(4)?.takeIf { it.length == 4 } ?: cur.pinCode,
                themePreset = map.get("themePreset") ?: cur.themePreset,
                bubbleCornerDp = map.get("bubbleCornerDp")?.toIntOrNull() ?: cur.bubbleCornerDp,
                chatFontScale = map.get("chatFontScale")?.toFloatOrNull() ?: cur.chatFontScale,
                antiScreenshotEnabled = map.get("antiScreenshotEnabled")?.toBooleanStrictOrNull() ?: cur.antiScreenshotEnabled,
                ambientPhotoGlow = map.get("ambientPhotoGlow")?.toBooleanStrictOrNull() ?: cur.ambientPhotoGlow
            )
        }
        return true
    }
}
