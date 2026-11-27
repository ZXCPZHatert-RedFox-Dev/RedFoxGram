package com.redfox.gram.engine

import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.bridge.RedFoxBridge
import com.redfox.gram.config.RedFoxSecrets
import com.redfox.gram.network.ProxyProtocol
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.ui.home.ChatPreview
import dev.g000sha256.tdl.TdlClient
import dev.g000sha256.tdl.TdlResult
import dev.g000sha256.tdl.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RedFoxTdEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: TdlClient? = null
    private var started = false

    private val _connectionStatus = MutableStateFlow("Подключение к Telegram...")
    val connectionStatus = _connectionStatus.asStateFlow()

    fun start(dbDirectory: String = "redfox_tdlib_db") {
        if (started) return
        started = true

        scope.launch {
            try {
                val tdl = TdlClient.create()
                client = tdl

                applyActiveProxy(tdl)

                launch {
                    tdl.authorizationStateUpdates.collect { update ->
                        handleAuthState(tdl, update.authorizationState, dbDirectory)
                    }
                }
            } catch (e: Throwable) {
                _connectionStatus.value = "Ошибка ядра TDLib: ${e.message}"
                RedFoxAuthManager.setAuthError("Ошибка инициализации TDLib: ${e.message}")
            }
        }
    }

    private suspend fun applyActiveProxy(tdl: TdlClient) {
        val proxyState = RedFoxProxyManager.state.value
        if (!proxyState.enabled) return
        val node = proxyState.nodes.find { it.id == proxyState.activeNodeId } ?: return

        val proxyType = when (node.protocol) {
            ProxyProtocol.MTPROTO -> ProxyTypeMtproto(secret = node.secret)
            ProxyProtocol.SOCKS5 -> ProxyTypeSocks5(username = "", password = "")
        }
        runCatching {
            tdl.addProxy(
                server = node.host,
                port = node.port,
                enable = true,
                type = proxyType
            )
        }
    }

    private suspend fun handleAuthState(tdl: TdlClient, state: AuthorizationState, dbDir: String) {
        when (state) {
            is AuthorizationStateWaitTdlibParameters -> {
                _connectionStatus.value = "Инициализация сессии..."
                tdl.setTdlibParameters(
                    useTestDc = false,
                    databaseDirectory = dbDir,
                    filesDirectory = "$dbDir/files",
                    databaseEncryptionKey = byteArrayOf(),
                    useFileDatabase = true,
                    useChatInfoDatabase = true,
                    useMessageDatabase = true,
                    useSecretChats = true,
                    apiId = RedFoxSecrets.API_ID,
                    apiHash = RedFoxSecrets.API_HASH,
                    systemLanguageCode = "ru",
                    deviceModel = "RedFoxGram M3E",
                    systemVersion = "Android 17 / Desktop",
                    applicationVersion = "0.2-M3E"
                )
            }
            is AuthorizationStateWaitPhoneNumber -> {
                _connectionStatus.value = "Готов к входу по номеру"
                RedFoxAuthManager.moveToStep(AuthStep.PHONE)
            }
            is AuthorizationStateWaitCode -> {
                val deliveryHint = when (state.codeInfo.type) {
                    is AuthenticationCodeTypeTelegramMessage -> "Код отправлен в чат Telegram на ваших других устройствах!"
                    is AuthenticationCodeTypeSms -> "Код отправлен по SMS (проверьте активные сессии Telegram)"
                    else -> "Код подтверждения отправлен в ваши активные сессии Telegram"
                }
                _connectionStatus.value = deliveryHint
                RedFoxAuthManager.setDeliveryHint(deliveryHint)
                RedFoxAuthManager.moveToStep(AuthStep.CODE)
            }
            is AuthorizationStateWaitPassword -> {
                _connectionStatus.value = "Требуется облачный пароль (2FA)"
                RedFoxAuthManager.moveToStep(AuthStep.PASSWORD_2FA)
            }
            is AuthorizationStateReady -> {
                _connectionStatus.value = "В сети (Telegram MTProto)"
                RedFoxAuthManager.completeLogin(skip2FA = true)
                loadRealChats(tdl)
            }
            else -> Unit
        }
    }

    private suspend fun loadRealChats(tdl: TdlClient) {
        runCatching {
            val chatsRes = tdl.getChats(chatList = ChatListMain(), limit = 40)
            val ids: LongArray = (chatsRes as? TdlResult.Success)?.result?.chatIds ?: return
            val previews = mutableListOf<ChatPreview>()
            for (chatId in ids) {
                val chat: Chat = (tdl.getChat(chatId = chatId) as? TdlResult.Success)?.result ?: continue
                val lastText = when (val content = chat.lastMessage?.content) {
                    is MessageText -> content.text.text
                    is MessagePhoto -> "Фотография"
                    is MessageVideo -> "Видео"
                    is MessageVoiceNote -> "Голосовое сообщение"
                    is MessageVideoNote -> "Видеосообщение (кружочек)"
                    is MessageSticker -> "Стикер ${content.sticker.emoji}"
                    else -> "Медиа / Сообщение"
                }
                previews.add(
                    ChatPreview(
                        id = chat.id,
                        title = chat.title,
                        lastMessage = lastText,
                        time = "Онлайн",
                        unreadCount = chat.unreadCount
                    )
                )
            }
            if (previews.isNotEmpty()) {
                RedFoxBridge.pushDialogsFromEngine(previews)
            }
        }
    }

    fun sendPhoneNumber(phone: String) {
        scope.launch {
            runCatching {
                val authSettings = PhoneNumberAuthenticationSettings(
                    allowFlashCall = false,
                    allowMissedCall = false,
                    isCurrentPhoneNumber = false,
                    hasUnknownPhoneNumber = false,
                    allowSmsRetrieverApi = false,
                    firebaseAuthenticationSettings = null,
                    authenticationTokens = emptyArray()
                )
                client?.setAuthenticationPhoneNumber(
                    phoneNumber = phone.replace(" ", ""),
                    settings = authSettings
                )
            }.onFailure {
                RedFoxAuthManager.setAuthError(it.message ?: "Ошибка отправки номера")
            }
        }
    }

    fun resendCode() {
        scope.launch {
            runCatching {
                client?.resendAuthenticationCode(reason = ResendCodeReasonUserRequest())
            }.onFailure {
                RedFoxAuthManager.setAuthError(it.message ?: "Не удалось запросить код повторно")
            }
        }
    }

    fun sendAuthCode(code: String) {
        scope.launch {
            runCatching {
                client?.checkAuthenticationCode(code = code)
            }.onFailure {
                RedFoxAuthManager.setAuthError(it.message ?: "Неверный код подтверждения")
            }
        }
    }

    fun sendPassword2FA(password: String) {
        scope.launch {
            runCatching {
                client?.checkAuthenticationPassword(password = password)
            }.onFailure {
                RedFoxAuthManager.setAuthError(it.message ?: "Неверный пароль 2FA")
            }
        }
    }

    fun logOut() {
        scope.launch {
            runCatching { client?.logOut() }
        }
    }
}
