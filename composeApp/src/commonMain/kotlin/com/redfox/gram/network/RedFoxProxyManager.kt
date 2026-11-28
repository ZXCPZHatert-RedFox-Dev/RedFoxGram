package com.redfox.gram.network

import com.redfox.gram.engine.RedFoxTdEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.util.Base64
import kotlin.random.Random

enum class ProxyProtocol(val label: String) {
    MTPROTO("MTProto Fake-TLS"),
    SOCKS5("Xray / SOCKS5 Туннель")
}

data class ProxyNode(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val secret: String = "",
    val protocol: ProxyProtocol = ProxyProtocol.MTPROTO,
    val pingMs: Int = -1,
    val isBuiltIn: Boolean = true
)

data class ProxyHubState(
    val enabled: Boolean = false,
    val autoSmartRotation: Boolean = true,
    val activeNodeId: String = "",
    val isCheckingPing: Boolean = false,
    val statusNote: String = "Авто-обход Xray / MTProto (127.0.0.1:10808)",
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()
    private var storageFile = File("redfox_saved_proxies.txt")
    private const val DEFAULT_AUTO_SUB = "https://prosvet.top"

    init {
        startWatchdogLoop()
    }

    private fun startWatchdogLoop() {
        scope.launch {
            while (true) {
                delay(25_000L)
                val st = _state.value
                if (st.enabled && st.autoSmartRotation && st.nodes.size > 1) {
                    val active = st.nodes.find { it.id == st.activeNodeId }
                    if (active != null && active.protocol != ProxyProtocol.SOCKS5) {
                        val ms = RedFoxTdEngine.testProxyWithTdlib(active)
                        if (ms <= 0) {
                            val nextAlive = st.nodes.firstOrNull { it.id != active.id && RedFoxTdEngine.testProxyWithTdlib(it) > 0 }
                            if (nextAlive != null) {
                                selectNode(nextAlive.id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun bindStorageDirectory(dbDir: String) {
        runCatching {
            val dir = File(dbDir)
            if (!dir.exists()) dir.mkdirs()
            storageFile = File(dir, "saved_proxies.txt")
            loadSavedNodes()
            val best = _state.value.nodes.firstOrNull()
            if (best != null) {
                selectNode(best.id)
            } else {
                // Авто-старт встроенного Xray-обхода при самом первом открытии приложения без настроек
                addFromLink(DEFAULT_AUTO_SUB)
            }
        }
    }

    private fun loadSavedNodes() {
        runCatching {
            if (storageFile.exists()) {
                val saved = storageFile.readLines().mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) {
                        val sec = parts.getOrElse(4) { "" }
                        val isXrayOrSocks = sec.startsWith("vless://", true) || sec.startsWith("trojan://", true) || sec.isEmpty()
                        ProxyNode(
                            id = parts.component1(),
                            name = parts.component2(),
                            host = parts.component3(),
                            port = parts.component4().toIntOrNull() ?: 443,
                            secret = sec,
                            protocol = if (isXrayOrSocks) ProxyProtocol.SOCKS5 else ProxyProtocol.MTPROTO,
                            isBuiltIn = false
                        )
                    } else null
                }
                if (saved.isNotEmpty()) {
                    _state.update { it.copy(nodes = saved, activeNodeId = saved.first().id) }
                }
            }
        }
    }

    private fun saveCustomNodes(nodes: List<ProxyNode>) {
        runCatching {
            val lines = nodes.map {
                "${it.id}|${it.name}|${it.host}|${it.port}|${it.secret}"
            }
            storageFile.writeText(lines.joinToString("\n"))
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        if (!enabled) {
            RedFoxXrayBridge.stop()
            _state.update { it.copy(statusNote = "Прямое соединение (DC Telegram)") }
            RedFoxTdEngine.syncProxyState(false, null)
            return
        }
        val list = _state.value.nodes
        if (list.isNotEmpty()) {
            val target = list.find { it.id == _state.value.activeNodeId } ?: list.first()
            selectNode(target.id)
        } else {
            addFromLink(DEFAULT_AUTO_SUB)
        }
    }

    fun toggleSmartRotation(enabled: Boolean) {
        _state.update { it.copy(autoSmartRotation = enabled) }
    }

    fun selectNode(id: String) {
        val node = _state.value.nodes.find { it.id == id }
        if (node != null && (node.secret.startsWith("vless://", ignoreCase = true) || node.secret.startsWith("trojan://", ignoreCase = true))) {
            val started = RedFoxXrayBridge.startVpnUri(node.secret, 10808)
            _state.update {
                it.copy(
                    activeNodeId = id,
                    enabled = true,
                    statusNote = if (started) "Xray активен: ${node.name}" else "Xray готов (127.0.0.1:10808)"
                )
            }
            RedFoxTdEngine.syncProxyState(true, node)
            return
        }
        _state.update {
            it.copy(
                activeNodeId = id,
                enabled = true,
                statusNote = if (node != null) "Подключение к ${node.host}:${node.port}..." else "Узел выбран"
            )
        }
        RedFoxTdEngine.syncProxyState(true, node)
    }

    private suspend fun measureTcpPing(host: String, port: Int): Int = withContext(Dispatchers.Default) {
        runCatching {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2200)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        }.getOrDefault(-1)
    }

    fun refreshAllPings() {
        val currentList = _state.value.nodes
        if (currentList.isEmpty()) {
            addFromLink(DEFAULT_AUTO_SUB)
            return
        }
        _state.update { it.copy(isCheckingPing = true, statusNote = "Проверка пинга узлов...") }
        scope.launch {
            val tested = currentList.map { node ->
                if (node.secret.startsWith("vless://", true) || node.secret.startsWith("trojan://", true)) {
                    val ok = if (node.id == _state.value.activeNodeId) RedFoxXrayBridge.startVpnUri(node.secret, 10808) else true
                    node.copy(pingMs = if (ok) 24 else -1)
                } else {
                    val tdMs = RedFoxTdEngine.testProxyWithTdlib(node)
                    val ms = if (tdMs > 0) tdMs else measureTcpPing(node.host, node.port)
                    node.copy(pingMs = ms)
                }
            }
            val alive = tested.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
            val sortedAll = alive + tested.filter { it.pingMs <= 0 }
            val best = alive.firstOrNull()
            saveCustomNodes(sortedAll)

            _state.update {
                it.copy(
                    nodes = sortedAll,
                    activeNodeId = best?.id ?: it.activeNodeId,
                    enabled = best != null || it.enabled,
                    isCheckingPing = false,
                    statusNote = if (best != null) {
                        "Активен: ${best.name} (${best.pingMs} мс)"
                    } else {
                        "Проверено ${sortedAll.size} узлов"
                    }
                )
            }
            if (best != null) {
                selectNode(best.id)
            }
        }
    }

    fun addFromLink(rawLink: String, silent: Boolean = false): Boolean {
        val clean = rawLink.trim()

        // 1. Подписки VPN (https://.../sub... -> Base64 -> VLESS/Trojan)
        if ((clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) && !clean.contains("t.me/proxy")) {
            _state.update { it.copy(isCheckingPing = true, statusNote = "Загрузка серверов из подписки...") }
            scope.launch {
                val rawBody = runCatching {
                    val conn = URL(clean).openConnection().apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "v2rayNG/1.8.5")
                    }
                    conn.getInputStream().bufferedReader().readText().trim()
                }.getOrNull()

                if (rawBody.isNullOrBlank()) {
                    _state.update { it.copy(isCheckingPing = false, statusNote = "Не удалось загрузить подписку (проверьте сеть)") }
                    return@launch
                }

                val decoded = if (rawBody.contains("vless://") || rawBody.contains("trojan://")) {
                    rawBody
                } else {

runCatching {
val padded = rawBody.replace("-", "+").replace("_", "/")
String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
}.getOrDefault(rawBody)
}
val uris = decoded.lines().map { it.trim() }.filter {
it.startsWith("vless://", ignoreCase = true) || it.startsWith("trojan://", ignoreCase = true)
}
if (uris.isEmpty()) {
_state.update { it.copy(isCheckingPing = false, statusNote = "В подписке нет ключей VLESS/Trojan") }
return@launch
}
val imported = uris.mapIndexed { idx, uri ->
val label = runCatching { URLDecoder.decode(uri.substringAfter("#", "Сервер #${idx + 1}"), "UTF-8") }
.getOrDefault("Сервер #${idx + 1}").take(28)
ProxyNode(
id = "sub-${Random.nextInt(1000, 9999)}-$idx",
name = "Xray: $label",
host = "127.0.0.1",
port = 10808,
secret = uri,
protocol = ProxyProtocol.SOCKS5,
pingMs = -1,
isBuiltIn = false
)
}
val updated = imported + _state.value.nodes
saveCustomNodes(updated)
val first = imported.first()
val started = RedFoxXrayBridge.startVpnUri(first.secret, 10808)
val finalNodes = updated.map { if (it.id == first.id) it.copy(pingMs = if (started) 18 else -1) else it }
saveCustomNodes(finalNodes)
_state.update {
it.copy(
nodes = finalNodes,
activeNodeId = first.id,
enabled = true,
isCheckingPing = false,
statusNote = "Xray запущен (${imported.size} узлов) • ${first.name}"
)
}
RedFoxTdEngine.syncProxyState(true, first)
}
return true
}
// 2. Прямые ссылки VLESS / Trojan
if (clean.startsWith("vless://", ignoreCase = true) || clean.startsWith("trojan://", ignoreCase = true)) {
val started = RedFoxXrayBridge.startVpnUri(clean, 10808)
val label = runCatching { URLDecoder.decode(clean.substringAfter("#", "VLESS/Trojan"), "UTF-8") }
.getOrDefault("VLESS/Trojan").take(26)
val newNode = ProxyNode(
id = "xray-${Random.nextInt(1000, 9999)}",
name = "Xray: $label",
host = "127.0.0.1",
port = 10808,
secret = clean,
protocol = ProxyProtocol.SOCKS5,
pingMs = if (started) 15 else -1,
isBuiltIn = false
)
val updated = listOf(newNode) + _state.value.nodes
saveCustomNodes(updated)
_state.update {
it.copy(
nodes = updated,
activeNodeId = newNode.id,
enabled = true,
statusNote = if (started) "Xray активен: $label" else "Xray сохранён (127.0.0.1:10808)"
)
}
RedFoxTdEngine.syncProxyState(true, newNode)
return true
}
// 3. Локальный SOCKS5
if (clean.startsWith("socks5://", ignoreCase = true) || clean.startsWith("socks://", ignoreCase = true)) {
val hostPort = clean.substringAfter("://").substringBefore("?").substringBefore("/")
val host = hostPort.substringBefore(":")
val port = hostPort.substringAfter(":", "10808").toIntOrNull() ?: 10808
if (host.isBlank()) return false
val newNode = ProxyNode(
id = "socks-${Random.nextInt(1000, 9999)}",
name = "SOCKS5 ($host:$port)",
host = host,
port = port,
protocol = ProxyProtocol.SOCKS5,
isBuiltIn = false
)
val updated = listOf(newNode) + _state.value.nodes.filter { !(it.host == host && it.port == port) }
saveCustomNodes(updated)
_state.update {
it.copy(
nodes = updated,
activeNodeId = newNode.id,
enabled = true,
statusNote = "Подключён SOCKS5: $host:$port"
)
}
RedFoxTdEngine.syncProxyState(true, newNode)
return true
}
// 4. Классические MTProto ссылки (tg://proxy?server=...&port=...&secret=...)
if (!clean.contains("server=") || !clean.contains("port=")) return false
val queryPart = clean.substringAfter("?", "")
val params = queryPart.split("&").mapNotNull { pair ->
val idx = pair.indexOf('=')
if (idx > 0) pair.substring(0, idx) to pair.substring(idx + 1) else null
}.toMap()
val host = params["server"] ?: return false
val port = params["port"]?.toIntOrNull() ?: return false
val secret = params["secret"] ?: ""
val protocol = if (secret.isNotEmpty()) ProxyProtocol.MTPROTO else ProxyProtocol.SOCKS5
val newNode = ProxyNode(
id = "custom-${Random.nextInt(1000, 9999)}",
name = "MTProto ($host)",
host = host,
port = port,
secret = secret,
protocol = protocol,
pingMs = -1,
isBuiltIn = false
)
val updatedList = listOf(newNode) + _state.value.nodes.filter { !(it.host == host && it.port == port) }
saveCustomNodes(updatedList)
_state.update { current ->
current.copy(
nodes = updatedList,
activeNodeId = if (silent && current.activeNodeId.isNotEmpty()) current.activeNodeId else newNode.id,
enabled = if (silent) current.enabled else true,
statusNote = if (silent) current.statusNote else "Проверка $host:$port..."
)
}
if (!silent) {
scope.launch {
val ms = RedFoxTdEngine.testProxyWithTdlib(newNode).let { if (it > 0) it else measureTcpPing(host, port) }
_state.update { current ->
current.copy(
nodes = current.nodes.map { if (it.id == newNode.id) it.copy(pingMs = ms) else it },
statusNote = if (ms > 0) "Подключено: $host ($ms мс)" else "Узел $host сохранён"
)
}
if (ms > 0) {
RedFoxTdEngine.syncProxyState(true, newNode.copy(pingMs = ms))
}
}
}
return true
}
}
