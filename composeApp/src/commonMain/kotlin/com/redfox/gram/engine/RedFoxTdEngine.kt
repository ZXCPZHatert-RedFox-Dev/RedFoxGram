package com.redfox.gram.engine

import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.bridge.RedFoxBridge
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.config.RedFoxSecrets
import com.redfox.gram.network.ProxyNode
import com.redfox.gram.network.ProxyProtocol
import com.redfox.gram.network.RedFoxProxyManager
import com.redfox.gram.ui.home.ChatPreview
import dev.g000sha256.tdl.TdlClient
import dev.g000sha256.tdl.TdlResult
import dev.g000sha256.tdl.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RedFoxTdEngine {
    data class CurrentUserProfile(
        val id: Long = 0L,
        val fullName: String = "Пользователь RedFoxGram",
        val username: String = "",
        val phone: String = "",
        val avatarPath: String = ""
    )

    private val _currentUser = MutableStateFlow(CurrentUserProfile())
    val currentUser = _currentUser.asStateFlow()

    data class SyncedTelegramTheme(
        val name: String,
        val primaryColorHex: Long,
        val containerColorHex: Long,
        val backgroundColorHex: Long
    )

    private val _syncedTelegramThemes = MutableStateFlow<List<SyncedTelegramTheme>>(emptyList())
    val syncedTelegramThemes = _syncedTelegramThemes.asStateFlow()


    private val _searchResults = MutableStateFlow<List<ChatPreview>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _chatAvatars = MutableStateFlow<Map<Long, String>>(emptyMap())
    val chatAvatars = _chatAvatars.asStateFlow()

    private val fileIdToChatId = mutableMapOf<Int, Long>()
    private val fileIdToMsgId = mutableMapOf<Int, Long>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: TdlClient? = null
    private var started = false

    private val _connectionStatus = MutableStateFlow("Ожидание запуска...")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _networkBanner = MutableStateFlow("Соединение...")
    val networkBanner = _networkBanner.asStateFlow()

    private val _isNetworkReady = MutableStateFlow(false)
    val isNetworkReady = _isNetworkReady.asStateFlow()

    private val _lastReadOutboxMap = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val lastReadOutboxMap = _lastReadOutboxMap.asStateFlow()

    data class LiveMessageItem(
        val id: Long,
        val senderName: String,
        val text: String,
        val isOutgoing: Boolean,
        val timeText: String,
        val isDeletedSaved: Boolean = false,
        val isEdited: Boolean = false,
        val isSentToServer: Boolean = true,
        val mediaLocalPath: String = "",
        val replyToText: String = ""
    )

    private val _activeChatMessages = MutableStateFlow<List<LiveMessageItem>>(emptyList())
    val activeChatMessages = _activeChatMessages.asStateFlow()

    private val _channelFeedPosts = MutableStateFlow<List<LiveMessageItem>>(emptyList())
    val channelFeedPosts = _channelFeedPosts.asStateFlow()

    private var currentOpenedChatId: Long = 0L
    private val deletedMessageIds = mutableSetOf<Long>()

    fun start(dbDirectory: String = "redfox_tdlib_db") {
        if (started) return
        started = true
        RedFoxProxyManager.bindStorageDirectory(dbDirectory)
        _connectionStatus.value = "Запуск ядра TDLib..."

        scope.launch {
            try {
                if (RedFoxSecrets.API_ID == 0 || RedFoxSecrets.API_HASH.isBlank()) {
                    _connectionStatus.value = "Ошибка: не заданы API_ID / API_HASH!"
                    RedFoxAuthManager.setAuthError("Ключи TELEGRAM_API_ID / API_HASH не заданы в сборке")
                    return@launch
                }

                val tdl = TdlClient.create()
                client = tdl
                runCatching { tdl.setLogVerbosityLevel(1) }

                // 0. Синхронизация реальных папок аккаунта Telegram
                launch {
                    tdl.chatFoldersUpdates.collect { update ->
                        val synced = update.chatFolders.map { info ->
                            TelegramFolderTab(id = info.id, title = info.name.text.text)
                        }
                        _telegramFolders.value = synced
                    }
                }

                // Слушатели статуса сети (Соединение / Обновление / ✓) и галочек прочтения
                launch {
                    tdl.connectionStateUpdates.collect { upd ->
                        when (upd.state) {
                            is ConnectionStateWaitingForNetwork -> {
                                _isNetworkReady.value = false
                                _networkBanner.value = "Ожидание сети..."
                            }
                            is ConnectionStateConnectingToProxy -> {
                                _isNetworkReady.value = false
                                _networkBanner.value = "Подключение к прокси..."
                            }
                            is ConnectionStateConnecting -> {
                                _isNetworkReady.value = false
                                _networkBanner.value = "Соединение..."
                            }
                            is ConnectionStateUpdating -> {
                                _isNetworkReady.value = false
                                _networkBanner.value = "Обновление..."
                            }
                            is ConnectionStateReady -> {
                                _isNetworkReady.value = true
                                _networkBanner.value = "✓"
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    tdl.chatReadOutboxUpdates.collect { upd ->
                        _lastReadOutboxMap.update { it + (upd.chatId to upd.lastReadOutboxMessageId) }
                    }
                }
                launch {
                    tdl.messageSendSucceededUpdates.collect { upd ->
                        val msg = upd.message
                        if (msg.chatId == currentOpenedChatId) {
                            _activeChatMessages.update { list ->
                                list.map {
                                    if (it.id == upd.oldMessageId) it.copy(id = msg.id, isSentToServer = true) else it
                                }
                            }
                        }
                    }
                }

                                // Синхронизация облачных тем аккаунта Telegram (chatThemesUpdates)
                launch {
                    tdl.chatThemesUpdates.collect { upd ->
                        val mapped = upd.chatThemes.mapNotNull { th ->
                            val dark = th.darkSettings ?: th.lightSettings
                            val accent = dark?.accentColor?.toLong()?.and(0xFFFFFFFFL) ?: 0xFFFFB4A1L
                            val bg = 0xFF141218L
                            val fillPrimary = 0xFF000000L or (accent and 0x00FFFFFFL)
                            val fillBg = 0xFF000000L or (bg and 0x00FFFFFFL)
                            val fillContainer = 0xFF1F1C24L
                            SyncedTelegramTheme(
                                name = "Тема TG ${th.name}",
                                primaryColorHex = fillPrimary,
                                containerColorHex = fillContainer,
                                backgroundColorHex = fillBg
                            )
                        }
                        if (mapped.isNotEmpty()) {
                            _syncedTelegramThemes.value = mapped
                        }
                    }
                }

                // 1. Слушатель авторизации
                launch {
                    tdl.authorizationStateUpdates.collect { update ->
                        handleAuthState(tdl, update.authorizationState, dbDirectory)
                    }
                }

                // 2. Мгновенные входящие сообщения в реальном времени
                launch {
                    tdl.newMessageUpdates.collect { update ->
                        val msg = update.message
                        if (msg.chatId == currentOpenedChatId) {
                            val mapped = mapTdMessage(msg)
                            _activeChatMessages.update { list ->
                                if (list.any { it.id == mapped.id }) list else list + mapped
                            }
                            if (!RedFoxConfig.state.value.ghostReadMessages && !msg.isOutgoing) {
                                runCatching {
                                    tdl.viewMessages(

chatId = msg.chatId,
messageIds = longArrayOf(msg.id),
source = null,
forceRead = true
)
}
}
}
loadRealChats(tdl)
                harvestProxiesFromTelegramChannels()
}
}
// 3. Реальный перехват удаленных сообщений (AyuGram / Opexgram)
launch {
tdl.deleteMessagesUpdates.collect { update ->
if (update.isPermanent && RedFoxConfig.state.value.antiDeleteMessages) {
deletedMessageIds.addAll(update.messageIds.toList())
if (update.chatId == currentOpenedChatId) {
_activeChatMessages.update { list ->
list.map { item ->
if (item.id in update.messageIds) item.copy(isDeletedSaved = true) else item
}
}
}
}
}
}
// 4. Отслеживание редактирования сообщений
launch {
tdl.messageContentUpdates.collect { update ->
if (update.chatId == currentOpenedChatId) {
val newText = extractContentText(update.newContent)
_activeChatMessages.update { list ->
list.map { item ->
if (item.id == update.messageId) {
item.copy(text = "$newText\n(было: ${item.text})", isEdited = true)
} else item
}
}
}
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
fun syncProxyState(enabled: Boolean, node: ProxyNode?) {
scope.launch {
val tdl = client ?: return@launch
if (!enabled || node == null) {
runCatching { tdl.disableProxy() }
return@launch
}
val proxyType = when (node.protocol) {
ProxyProtocol.MTPROTO -> ProxyTypeMtproto(secret = node.secret)
ProxyProtocol.SOCKS5 -> ProxyTypeSocks5(username = "", password = "")
}
runCatching {
tdl.addProxy(server = node.host, port = node.port, enable = true, type = proxyType)
}
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
systemVersion = "Android 17",
applicationVersion = "0.3-M3E"
)
}
is AuthorizationStateWaitPhoneNumber -> {
_connectionStatus.value = "Готов к входу • Введите номер"
RedFoxAuthManager.moveToStep(AuthStep.PHONE)
}
is AuthorizationStateWaitCode -> {
val deliveryHint = "Код отправлен в чат Telegram на ваших устройствах!"
_connectionStatus.value = deliveryHint
RedFoxAuthManager.setDeliveryHint(deliveryHint)
RedFoxAuthManager.moveToStep(AuthStep.CODE)
}
is AuthorizationStateWaitOtherDeviceConfirmation -> {
                _qrLoginLink.value = state.link
                _connectionStatus.value = "Отсканируйте QR-код с другого устройства Telegram"
            }
            is AuthorizationStateWaitPassword -> {
_connectionStatus.value = "Требуется облачный пароль (2FA)"
RedFoxAuthManager.moveToStep(AuthStep.PASSWORD_2FA)
}
is AuthorizationStateReady -> {
                                loadCurrentUserProfile()
_connectionStatus.value = "В сети (Telegram MTProto)"
RedFoxAuthManager.completeLogin(skip2FA = true)
loadRealChats(tdl)
}
else -> Unit
}
}
private suspend fun loadRealChats(tdl: TdlClient) {
runCatching {
val chatsRes = tdl.getChats(chatList = ChatListMain(), limit = 50)
val ids: LongArray = (chatsRes as? TdlResult.Success)?.result?.chatIds ?: return
val previews = mutableListOf<ChatPreview>()
val feedCollector = mutableListOf<LiveMessageItem>()
for (chatId in ids) {
val chat: Chat = (tdl.getChat(chatId = chatId) as? TdlResult.Success)?.result ?: continue
val lastMsg = chat.lastMessage
val lastText = if (lastMsg != null) extractContentText(lastMsg.content) else "Нет сообщений"
val isChannel = (chat.type as? ChatTypeSupergroup)?.isChannel == true
previews.add(
ChatPreview(
id = chat.id,
title = chat.title,
lastMessage = lastText,
time = if (isChannel) "Канал" else "Онлайн",
unreadCount = chat.unreadCount
)
)
if (isChannel && lastMsg != null) {
feedCollector.add(
LiveMessageItem(
id = lastMsg.id,
senderName = chat.title,
text = lastText,
isOutgoing = false,
timeText = "Пост канала"
)
)
}
}
if (previews.isNotEmpty()) {
RedFoxBridge.pushDialogsFromEngine(previews)
}
if (feedCollector.isNotEmpty()) {
_channelFeedPosts.value = feedCollector
}
}
}
fun openChatHistory(chatId: Long) {
currentOpenedChatId = chatId
_activeChatMessages.value = emptyList()
scope.launch {
val tdl = client ?: return@launch
val chatObj = (tdl.getChat(chatId = chatId) as? TdlResult.Success)?.result
            if (chatObj != null) {
                _lastReadOutboxMap.update { it + (chatId to chatObj.lastReadOutboxMessageId) }
            }
            val histRes = tdl.getChatHistory(
chatId = chatId,
fromMessageId = 0L,
offset = 0,
limit = 45,
onlyLocal = false
)
val messages = (histRes as? TdlResult.Success)?.result?.messages ?: return@launch
val items = messages.filterNotNull().map { msg -> mapTdMessage(msg) }.reversed()
_activeChatMessages.value = items
if (!RedFoxConfig.state.value.ghostReadMessages && items.isNotEmpty()) {
runCatching {
tdl.viewMessages(
chatId = chatId,
messageIds = items.map { it.id }.toLongArray(),
source = null,
forceRead = true
)
}
}
}
}

    fun sendMediaFile(chatId: Long, localPath: String, isImage: Boolean, caption: String = "") {
        scope.launch {
            val tdl = client ?: return@launch
            val inputFile = InputFileLocal(path = localPath)
            val formattedCaption = FormattedText(text = caption, entities = emptyArray())
            val content: InputMessageContent = if (isImage) {
                InputMessagePhoto(
                    photo = inputFile,
                    thumbnail = null,
                    addedStickerFileIds = intArrayOf(),
                    width = 0,
                    height = 0,
                    caption = formattedCaption,
                    showCaptionAboveMedia = false,
                    selfDestructType = null,
                    hasSpoiler = false
                )
            } else {
                InputMessageDocument(
                    document = inputFile,
                    thumbnail = null,
                    disableContentTypeDetection = false,
                    caption = formattedCaption
                )
            }
            tdl.sendMessage(
                chatId = chatId,
                messageThreadId = 0L,
                replyTo = null,
                options = null,
                replyMarkup = null,
                inputMessageContent = content
            )
        }
    }

    
    fun sendReplyMessage(chatId: Long, replyToMessageId: Long, text: String) {
        if (text.isBlank()) return
        scope.launch {
            val tdl = client ?: return@launch
            val content = InputMessageText(
                text = FormattedText(text = text, entities = emptyArray()),
                linkPreviewOptions = null,
                clearDraft = true
            )
            val replyTarget = InputMessageReplyToMessage(messageId = replyToMessageId, quote = null, checklistTaskId = 0)
            tdl.sendMessage(
                chatId = chatId,
                messageThreadId = 0L,
                replyTo = replyTarget,
                options = null,
                replyMarkup = null,
                inputMessageContent = content
            )
        }
    }

    fun editTextMessage(chatId: Long, messageId: Long, newText: String) {
        if (newText.isBlank()) return
        scope.launch {
            val tdl = client ?: return@launch
            val content = InputMessageText(
                text = FormattedText(text = newText, entities = emptyArray()),
                linkPreviewOptions = null,
                clearDraft = true
            )
            tdl.editMessageText(chatId = chatId, messageId = messageId, replyMarkup = null, inputMessageContent = content)
            _activeChatMessages.update { list ->
                list.map { if (it.id == messageId) it.copy(text = newText, isEdited = true) else it }
            }
        }
    }

    fun deleteChatMessage(chatId: Long, messageId: Long, revoke: Boolean = true) {
        scope.launch {
            client?.deleteMessages(chatId = chatId, messageIds = longArrayOf(messageId), revoke = revoke)
            _activeChatMessages.update { list -> list.filterNot { it.id == messageId } }
        }
    }

    
    fun searchTelegramChats(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        scope.launch {
            val tdl = client ?: return@launch
            val foundIds = linkedSetOf<Long>()
            val localRes = tdl.searchChats(query = clean, limit = 25) as? TdlResult.Success
            localRes?.result?.chatIds?.forEach { foundIds.add(it) }
            val globalRes = tdl.searchPublicChats(query = clean) as? TdlResult.Success
            globalRes?.result?.chatIds?.forEach { foundIds.add(it) }

            val list = mutableListOf<ChatPreview>()
            for (cid in foundIds) {
                val chat = (tdl.getChat(chatId = cid) as? TdlResult.Success)?.result ?: continue
                val lastMsg = chat.lastMessage
                val lastText = if (lastMsg != null) extractContentText(lastMsg.content) else "Публичный чат / канал"
                val isChannel = (chat.type as? ChatTypeSupergroup)?.isChannel == true
                list.add(
                    ChatPreview(
                        id = chat.id,
                        title = chat.title,
                        lastMessage = lastText,
                        time = if (isChannel) "Канал" else "Поиск",
                        unreadCount = chat.unreadCount
                    )
                )
            }
            _searchResults.value = list
        }
    }

    fun sendTextMessage(chatId: Long, text: String) {
if (text.isBlank()) return
scope.launch {
val tdl = client ?: return@launch
val content = InputMessageText(
text = FormattedText(text = text, entities = emptyArray()),
linkPreviewOptions = null,
clearDraft = true
)
tdl.sendMessage(
chatId = chatId,
messageThreadId = 0L,
replyTo = null,
options = null,
replyMarkup = null,
inputMessageContent = content
)
}
}
private fun extractContentText(c: MessageContent): String = when (c) {
is MessageText -> c.text.text
is MessagePhoto -> "🖼 Фото ${c.caption.text}".trim()
is MessageVideo -> "🎬 Видео (${c.video.duration}с) ${c.caption.text}".trim()
is MessageVoiceNote -> "🎤 Голосовое (${c.voiceNote.duration}с)"
is MessageVideoNote -> "⭕ Кружочек (${c.videoNote.duration}с)"
is MessageSticker -> "${c.sticker.emoji} Стикер"
is MessageAnimation -> "🎞 GIF ${c.caption.text}".trim()
is MessageAudio -> "🎵 ${c.audio.performer} — ${c.audio.title}"
is MessageDocument -> "📎 ${c.document.fileName}"
is MessageCall -> "📞 Звонок (${c.duration}с)"
is MessagePoll -> "📊 Опрос: ${c.poll.question.text}"
else -> "✨ Медиа / Вложение"
}
private fun mapTdMessage(msg: Message): LiveMessageItem {
return LiveMessageItem(
id = msg.id,
senderName = if (msg.isOutgoing) "Вы" else "Собеседник",
text = extractContentText(msg.content),
isOutgoing = msg.isOutgoing,
timeText = "TG",
isDeletedSaved = msg.id in deletedMessageIds
)
}

    private val _qrLoginLink = MutableStateFlow<String?>(null)
    val qrLoginLink = _qrLoginLink.asStateFlow()

    fun requestQrLogin() {
        scope.launch {
            val tdl = client ?: return@launch
            _connectionStatus.value = "Генерация QR-кода для входа..."
            val res = tdl.requestQrCodeAuthentication(otherUserIds = longArrayOf())
            if (res is TdlResult.Failure) {
                RedFoxAuthManager.setAuthError("Ошибка QR: ${res.message}")
            }
        }
    }

    suspend fun testProxyWithTdlib(node: ProxyNode): Int {
        val tdl = client ?: return -1
        val proxyType = when (node.protocol) {
            ProxyProtocol.MTPROTO -> ProxyTypeMtproto(secret = node.secret)
            ProxyProtocol.SOCKS5 -> ProxyTypeSocks5(username = "", password = "")
        }
        val addRes = tdl.addProxy(server = node.host, port = node.port, enable = false, type = proxyType)
        val proxyObj = (addRes as? TdlResult.Success)?.result ?: return -1
        val pingRes = tdl.pingProxy(proxyId = proxyObj.id)
        val seconds = (pingRes as? TdlResult.Success)?.result?.seconds ?: return -1
        return (seconds * 1000.0).toInt().coerceAtLeast(1)
    }

    fun sendPhoneNumber(phone: String) {
scope.launch {
val tdl = client ?: return@launch
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
RedFoxAuthManager.setAuthError("Ошибка (${res.code}): ${res.message}")
}
}
}
fun resendCode() {
scope.launch {
client?.resendAuthenticationCode(reason = ResendCodeReasonUserRequest())
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

    fun harvestProxiesFromTelegramChannels() {
        scope.launch {
            val tdl = client ?: return@launch
            val channels = listOf("ProxyMTProto", "mtproto_proxy_list", "TelMTProto")
            val linkRegex = Regex("""(?:tg://proxy|t\.me/proxy)\?server=([a-zA-Z0-9.\-]+)&port=(\d+)&secret=([a-zA-Z0-9_\-=]+)""")
            for (username in channels) {
                val chatRes = tdl.searchPublicChat(username = username)
                val chat = (chatRes as? TdlResult.Success)?.result ?: continue
                val histRes = tdl.getChatHistory(chatId = chat.id, fromMessageId = 0L, offset = 0, limit = 15, onlyLocal = false)
                val msgs = (histRes as? TdlResult.Success)?.result?.messages ?: continue
                for (m in msgs.filterNotNull()) {
                    val txt = extractContentText(m.content)
                    linkRegex.findAll(txt).forEach { match ->
                        RedFoxProxyManager.addFromLink(match.value, silent = true)
                    }
                }
            }
        }
    }

    
    data class TelegramFolderTab(
        val id: Int,
        val title: String
    )

    private val _telegramFolders = MutableStateFlow<List<TelegramFolderTab>>(emptyList())
    val telegramFolders = _telegramFolders.asStateFlow()

    private val _folderChats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val folderChats = _folderChats.asStateFlow()

    fun selectTelegramFolder(folderId: Int) {
        scope.launch {
            val tdl = client ?: return@launch
            _folderChats.value = emptyList()
            val chatsRes = tdl.getChats(chatList = ChatListFolder(chatFolderId = folderId), limit = 50)
            val ids = (chatsRes as? TdlResult.Success)?.result?.chatIds ?: return@launch
            val list = mutableListOf<ChatPreview>()
            for (cid in ids) {
                val chat = (tdl.getChat(chatId = cid) as? TdlResult.Success)?.result ?: continue
                val lastMsg = chat.lastMessage
                val lastText = if (lastMsg != null) extractContentText(lastMsg.content) else "Нет сообщений"
                val isChannel = (chat.type as? ChatTypeSupergroup)?.isChannel == true
                list.add(
                    ChatPreview(
                        id = chat.id,
                        title = chat.title,
                        lastMessage = lastText,
                        time = if (isChannel) "Канал" else "Онлайн",
                        unreadCount = chat.unreadCount
                    )
                )
            }
            _folderChats.value = list
        }
    }

    
    fun ensureChatAvatar(chatId: Long) {
        if (_chatAvatars.value.containsKey(chatId)) return
        scope.launch {
            val tdl = client ?: return@launch
            val chat = (tdl.getChat(chatId = chatId) as? TdlResult.Success)?.result ?: return@launch
            val photoFile = chat.photo?.small ?: return@launch
            if (photoFile.local.isDownloadingCompleted && photoFile.local.path.isNotBlank()) {
                _chatAvatars.update { it + (chatId to photoFile.local.path) }
            } else if (photoFile.id != 0) {
                fileIdToChatId.put(photoFile.id, chatId)
                val dl = (tdl.downloadFile(fileId = photoFile.id, priority = 12, offset = 0L, limit = 0L, synchronous = true) as? TdlResult.Success)?.result
                if (dl != null && dl.local.path.isNotBlank()) {
                    _chatAvatars.update { it + (chatId to dl.local.path) }
                }
            }
        }
    }

    
    fun loadCurrentUserProfile() {
        scope.launch {
            val tdl = client ?: return@launch
            val me = (tdl.getMe() as? TdlResult.Success)?.result ?: return@launch
            val name = listOf(me.firstName, me.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Telegram User" }
            val uname = me.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" } ?: ""
            val phoneStr = if (me.phoneNumber.isNotBlank()) "+${me.phoneNumber}" else ""
            var avPath = ""
            val photoFile = me.profilePhoto?.small
            if (photoFile != null) {
                if (photoFile.local.isDownloadingCompleted && photoFile.local.path.isNotBlank()) {
                    avPath = photoFile.local.path
                } else if (photoFile.id != 0) {
                    val dl = (tdl.downloadFile(fileId = photoFile.id, priority = 16, offset = 0L, limit = 0L, synchronous = true) as? TdlResult.Success)?.result
                    if (dl != null && dl.local.path.isNotBlank()) avPath = dl.local.path
                }
            }
            _currentUser.value = CurrentUserProfile(
                id = me.id,
                fullName = name,
                username = uname,
                phone = phoneStr,
                avatarPath = avPath
            )
        }
    }

    fun refreshChatsNow() {
scope.launch { client?.let { loadRealChats(it) } }
}
fun logOut() {
scope.launch { runCatching { client?.logOut() } }
}
}
