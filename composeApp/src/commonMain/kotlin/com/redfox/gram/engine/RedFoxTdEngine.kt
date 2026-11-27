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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: TdlClient? = null
    private var started = false

    private val _connectionStatus = MutableStateFlow("Ожидание запуска...")
    val connectionStatus = _connectionStatus.asStateFlow()

    data class LiveMessageItem(
        val id: Long,
        val senderName: String,
        val text: String,
        val isOutgoing: Boolean,
        val timeText: String,
        val isDeletedSaved: Boolean = false,
        val isEdited: Boolean = false
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
fun refreshChatsNow() {
scope.launch { client?.let { loadRealChats(it) } }
}
fun logOut() {
scope.launch { runCatching { client?.logOut() } }
}
}
