package com.redfox.gram.engine

import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.config.RedFoxSecrets
import com.redfox.gram.network.ProxyProtocol
import com.redfox.gram.network.RedFoxProxyManager
import dev.g000sha256.tdl.TdlClient
import dev.g000sha256.tdl.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RedFoxTdEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: TdlClient? = null
    private var started = false

    private val _connectionStatus = MutableStateFlow("Ожидание инициализации...")
    val connectionStatus = _connectionStatus.asStateFlow()

    fun start(dbDirectory: String = "redfox_tdlib") {
        if (started) return
        started = true

        scope.launch {
            try {
                val tdl = TdlClient.create()
                client = tdl

                // Применяем активный MTProto / SOCKS5 прокси из RedFox Shield
                applyActiveProxy(tdl)

                // Слушаем обновления авторизации от серверов Telegram
                launch {
                    tdl.authorizationStateUpdates.collect { update ->
                        handleAuthState(tdl, update.authorizationState, dbDirectory)
                    }
                }
            } catch (e: Throwable) {
                _connectionStatus.value = "Ошибка ядра TDLib: ${e.message}"
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
                _connectionStatus.value = "Передача параметров API..."
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
            }
            is AuthorizationStateWaitCode -> {
                _connectionStatus.value = "Ожидание 5-значного кода"
            }
            is AuthorizationStateWaitPassword -> {
                _connectionStatus.value = "Ожидание пароля 2FA"
            }
            is AuthorizationStateReady -> {
                _connectionStatus.value = "Подключено к Telegram"
                RedFoxAuthManager.completeLogin(skip2FA = true)
            }
            else -> Unit
        }
    }

    fun sendPhoneNumber(phone: String) {
        scope.launch {
            runCatching {
                client?.setAuthenticationPhoneNumber(
                    phoneNumber = phone.replace(" ", ""),
                    settings = null
                )
            }
        }
    }

    fun sendAuthCode(code: String) {
        scope.launch {
            runCatching {
                client?.checkAuthenticationCode(code = code)
            }
        }
    }

    fun sendPassword2FA(password: String) {
        scope.launch {
            runCatching {
                client?.checkAuthenticationPassword(password = password)
            }
        }
    }
}
