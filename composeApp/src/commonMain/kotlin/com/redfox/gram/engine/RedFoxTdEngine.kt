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

    private val _connectionStatus = MutableStateFlow("Ожидание запуска...")
    val connectionStatus = _connectionStatus.asStateFlow()

    fun start(dbDirectory: String = "redfox_tdlib_db") {
        if (started) return
        started = true
        _connectionStatus.value = "Запуск корутины TDLib..."

        scope.launch {
            try {
                if (RedFoxSecrets.API_ID == 0 || RedFoxSecrets.API_HASH.isBlank()) {
                    _connectionStatus.value = "Ошибка: API_ID=${RedFoxSecrets.API_ID} (пусто)"
                    RedFoxAuthManager.setAuthError("Ключи TELEGRAM_API_ID / API_HASH не заданы в сборке")
                    return@launch
                }

                _connectionStatus.value = "Создание TdlClient (ARMv8)..."
                val tdl = TdlClient.create()
                client = tdl

                _connectionStatus.value = "Настройка сети и прокси..."
                runCatching { tdl.setLogVerbosityLevel(1) }
                applyActiveProxy(tdl)

                launch {
                    tdl.authorizationStateUpdates.collect { update ->
                        handleAuthState(tdl, update.authorizationState, dbDirectory)
                    }
                }

                val initialRes = tdl.getAuthorizationState()
                val initialState = (initialRes as? TdlResult.Success)?.result
                if (initialState != null) {
                    handleAuthState(tdl, initialState, dbDirectory)
                }
            } catch (e: Throwable) {
                val errDetail = "${e::class.simpleName}: ${e.message}"
                _connectionStatus.value = "Ошибка: $errDetail"
                RedFoxAuthManager.setAuthError(errDetail)
            }
        }
    }

    private suspend fun applyActiveProxy(tdl: TdlClient) {
        val proxyState = RedFoxProxyManager.state.value
        if (!proxyState.enabled || proxyState.nodes.isEmpty()) return
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
                _connectionStatus.value = "Передача параметров API (${RedFoxSecrets.API_ID})..."
                val res = tdl.setTdlibParameters(
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
                    systemVersion = "Android 17",
                    applicationVersion = "0.2-M3E"
                )
                if (res is TdlResult.Failure) {
                    _connectionStatus.value = "Ошибка параметров: ${res.message}"
                    RedFoxAuthManager.setAuthError(res.message)
                }
            }
            is AuthorizationStateWaitPhoneNumber -> {
                _connectionStatus.value = "Готов к входу • Введите номер"
                RedFoxAuthManager.moveToStep(AuthStep.PHONE)
            }
            is AuthorizationStateWaitCode -> {
                val deliveryHint = when (state.codeInfo.type) {
                    is AuthenticationCodeTypeTelegramMessage -> "Код отправлен в чат Telegram на ваших устройствах!"
                    is AuthenticationCodeTypeSms -> "Код отправлен по SMS"
                    else -> "Код отправлен в ваши активные сессии Telegram"
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
            val tdl = client
            if (tdl == null) {
                RedFoxAuthManager.setAuthError("Ядро TDLib не запущено: ${_connectionStatus.value}")
                return@launch
            }
            _connectionStatus.value = "Отправка номера на серверы Telegram..."
            val authSettings = PhoneNumberAuthenticationSettings(
                allowFlashCall = false,
                allowMissedCall = false,
                isCurrentPhoneNumber = false,
                hasUnknownPhoneNumber = false,
                allowSmsRetrieverApi = false,
                firebaseAuthenticationSettings = null,
                authenticationTokens = emptyArray()
            )
            val res = tdl.setAuthenticationPhoneNumber(
                phoneNumber = phone.replace(" ", ""),
                settings = authSettings
            )
            if (res is TdlResult.Failure) {
                _connectionStatus.value = "Ошибка Telegram: ${res.message}"
                RedFoxAuthManager.setAuthError("Ошибка (${res.code}): ${res.message}")
            }
        }
    }

    fun resendCode() {
        scope.launch {
            val res = client?.resendAuthenticationCode(reason = ResendCodeReasonUserRequest())
            if (res is TdlResult.Failure) {
                RedFoxAuthManager.setAuthError(res.message)
            }
        }
    }

    fun sendAuthCode(code: String) {
        scope.launch {
            val res = client?.checkAuthenticationCode(code = code)

if (res is TdlResult.Failure) {
RedFoxAuthManager.setAuthError("Неверный код: ${res.message}")
}
}
}
fun sendPassword2FA(password: String) {
scope.launch {
val res = client?.checkAuthenticationPassword(password = password)
if (res is TdlResult.Failure) {
RedFoxAuthManager.setAuthError("Неверный пароль 2FA: ${res.message}")
}
}
}
fun logOut() {
scope.launch {
runCatching { client?.logOut() }
}
}

    fun refreshChatsNow() {
        scope.launch { client?.let { loadRealChats(it) } }
    }
}
