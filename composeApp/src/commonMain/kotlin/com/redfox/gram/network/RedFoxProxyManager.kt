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
import kotlin.random.Random

enum class ProxyProtocol(val label: String) {
    MTPROTO("MTProto Fake-TLS"),
    SOCKS5("SOCKS5 / Локальный туннель")
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
    val statusNote: String = "Авто-ротация MTProto / SOCKS5 (127.0.0.1)",
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()
    private var storageFile = File("redfox_saved_proxies.txt")

    init {
        startWatchdogLoop()
    }

    // Сторожевой таймер (Watchdog): при включённой авто-ротации каждые 25 сек проверяет связь и переключает на живой узел
    private fun startWatchdogLoop() {
        scope.launch {
            while (true) {
                delay(25_000L)
                val st = _state.value
                if (st.enabled && st.autoSmartRotation && st.nodes.size > 1) {
                    val active = st.nodes.find { it.id == st.activeNodeId }
                    if (active != null) {
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
                _state.update {
                    it.copy(
                        enabled = true,
                        activeNodeId = best.id,
                        statusNote = "Авто-старт из кэша: ${best.host}:${best.port}"
                    )
                }
                RedFoxTdEngine.syncProxyState(true, best)
            }
        }
    }

    private fun loadSavedNodes() {
        runCatching {
            if (storageFile.exists()) {
                val saved = storageFile.readLines().mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) {
                        ProxyNode(
                            id = parts.component1(),
                            name = parts.component2(),
                            host = parts.component3(),
                            port = parts.component4().toIntOrNull() ?: 443,
                            secret = parts.getOrElse(4) { "" },
                            protocol = if (parts.getOrElse(4) { "" }.isNotEmpty()) ProxyProtocol.MTPROTO else ProxyProtocol.SOCKS5,
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
            _state.update { it.copy(statusNote = "Прямое соединение (DC Telegram)") }
            RedFoxTdEngine.syncProxyState(false, null)
            return
        }
        val list = _state.value.nodes
        if (list.isNotEmpty()) {
            val target = list.find { it.id == _state.value.activeNodeId } ?: list.first()
            selectNode(target.id)
        } else {
            refreshAllPings()
        }
    }

    fun toggleSmartRotation(enabled: Boolean) {
        _state.update { it.copy(autoSmartRotation = enabled) }
    }

    fun selectNode(id: String) {
        val node = _state.value.nodes.find { it.id == id }
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
        _state.update { it.copy(isCheckingPing = true, statusNote = "Сбор из каналов Telegram и замер пинга...") }
        RedFoxTdEngine.harvestProxiesFromTelegramChannels()
        scope.launch {
            delay(1800L)
            val currentList = _state.value.nodes
            if (currentList.isEmpty()) {
                val dcPing = measureTcpPing("149.154.167.50", 443)
                _state.update {
                    it.copy(
                        isCheckingPing = false,
                        statusNote = if (dcPing > 0) "Прямой канал DC Telegram ($dcPing мс) доступен" else "Добавьте ссылку tg://proxy или socks5://127.0.0.1:10808"
                    )
                }
                return@launch
            }
            val tested = currentList.map { node ->
                val tdMs = RedFoxTdEngine.testProxyWithTdlib(node)
                val ms = if (tdMs > 0) tdMs else measureTcpPing(node.host, node.port)
                node.copy(pingMs = ms)
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
"Живых: ${alive.size}/${sortedAll.size} • ${best.host} (${best.pingMs} мс)"
} else {
"Проверено ${sortedAll.size} узлов • вставьте рабочий tg://proxy или socks5"
}
)
}
if (best != null) {
RedFoxTdEngine.syncProxyState(true, best)
}
}
}
fun addFromLink(rawLink: String, silent: Boolean = false): Boolean {
val clean = rawLink.trim()
// Поддержка и tg://proxy?server=...&port=...&secret=..., и socks5://host:port
if (clean.startsWith("socks5://", ignoreCase = true) || clean.startsWith("socks://", ignoreCase = true)) {
val hostPort = clean.substringAfter("://").substringBefore("?").substringBefore("/")
val host = hostPort.substringBefore(":")
val port = hostPort.substringAfter(":", "10808").toIntOrNull() ?: 10808
if (host.isBlank()) return false
val newNode = ProxyNode(
id = "socks-${Random.nextInt(1000, 9999)}",
name = "SOCKS5 Туннель ($host:$port)",
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
statusNote = "Подключён локальный SOCKS5: $host:$port"
)
}
RedFoxTdEngine.syncProxyState(true, newNode)
return true
}
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
