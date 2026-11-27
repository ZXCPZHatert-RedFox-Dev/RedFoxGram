package com.redfox.gram.network

import com.redfox.gram.engine.RedFoxTdEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.random.Random

enum class ProxyProtocol(val label: String) {
    MTPROTO("MTProto Fake-TLS"),
    SOCKS5("SOCKS5")
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
    val statusNote: String = "Нажмите тумблер или «Проверить пинг» для поиска серверов",
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()

    // Резервный встроенный пул на случай, если GitHub/CDN заблокированы провайдером
    private val fallbackPublicNodes = listOf(
        ProxyNode("fb-1", "EU Frankfurt #1 (TLS)", "mtproto.info", 443, "ee00000000000000000000000000000000676f6f676c652e636f6d", ProxyProtocol.MTPROTO),
        ProxyNode("fb-2", "RU/EU Bridge #2 (TLS)", "proxy.digitalresistance.dog", 443, "ddd41d8cd98f00b204e9800998ecf8427e", ProxyProtocol.MTPROTO),
        ProxyNode("fb-3", "NL Amsterdam #3 (TLS)", "nl.tgproxy.today", 443, "ee1603010200010001fc030386e24c3add636c6f7564666c6172652e636f6d", ProxyProtocol.MTPROTO),
        ProxyNode("fb-4", "FI Helsinki #4 (TLS)", "fi.tgproxy.today", 443, "ee1603010200010001fc030386e24c3add636c6f7564666c6172652e636f6d", ProxyProtocol.MTPROTO)
    )

    fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        if (!enabled) {
            _state.update { it.copy(statusNote = "Прямое соединение") }
            RedFoxTdEngine.syncProxyState(false, null)
            return
        }
        if (_state.value.nodes.isEmpty()) {
            fetchPublicMtprotoNodes()
        } else {
            val active = _state.value.nodes.find { it.id == _state.value.activeNodeId }
                ?: _state.value.nodes.firstOrNull()
            if (active != null) {
                _state.update { it.copy(activeNodeId = active.id, statusNote = "Подключено: ${active.host}:${active.port}") }
                RedFoxTdEngine.syncProxyState(true, active)
            }
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
                statusNote = if (node != null) "Активен: ${node.host}:${node.port}" else "Узел выбран"
            )
        }
        RedFoxTdEngine.syncProxyState(true, node)
    }

    private suspend fun measureTcpPing(host: String, port: Int): Int = withContext(Dispatchers.Default) {
        runCatching {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        }.getOrDefault(-1)
    }

    fun refreshAllPings() {
        if (_state.value.nodes.isEmpty()) {
            fetchPublicMtprotoNodes()
            return
        }
        _state.update { it.copy(isCheckingPing = true, statusNote = "Замер TCP-пинга серверов...") }
        scope.launch {
            val tested = _state.value.nodes.map { node ->
                val ms = measureTcpPing(node.host, node.port)
                node.copy(pingMs = ms)
            }
            val alive = tested.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
            val finalNodes = if (alive.isNotEmpty()) tested.sortedBy { if (it.pingMs > 0) it.pingMs else 99999 } else tested
            val bestId = if (_state.value.autoSmartRotation && alive.isNotEmpty()) {
                alive.first().id
            } else {
                _state.value.activeNodeId.ifEmpty { finalNodes.firstOrNull()?.id ?: "" }
            }
            val active = finalNodes.find { it.id == bestId }
            _state.update {
                it.copy(
                    nodes = finalNodes,
                    activeNodeId = bestId,
                    isCheckingPing = false,
                    statusNote = if (active != null && active.pingMs > 0) {
                        "Живых: ${alive.size}/${finalNodes.size} • ${active.host} (${active.pingMs} мс)"
                    } else {
                        "Проверено узлов: ${finalNodes.size} • вставьте свой tg://proxy для лучшей скорости"
                    }
                )
            }
            if (_state.value.enabled) {
                RedFoxTdEngine.syncProxyState(true, active)
            }
        }
    }

    fun fetchPublicMtprotoNodes() {
        _state.update { it.copy(isCheckingPing = true, statusNote = "Поиск MTProto-прокси в сети...") }
        scope.launch {
            val candidates = mutableListOf<ProxyNode>()
            val sources = listOf(
                "https://jsdelivr.net",
                "https://jsdelivr.net",
                "https://jsdelivr.net",
                "https://githubusercontent.com"
            )
            for (src in sources) {
                runCatching {
                    val conn = URL(src).openConnection().apply {
                        connectTimeout = 3500
                        readTimeout = 3500
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    val body = conn.getInputStream().bufferedReader().readText()

                    val linkRegex = Regex("""server=([a-zA-Z0-9.\-]+)&port=(\d+)&secret=([a-zA-Z0-9_\-=]+)""")
                    linkRegex.findAll(body).take(20).forEach { m ->
                        val (host, portStr, secret) = m.destructured
                        val port = portStr.toIntOrNull() ?: 443
                        if (candidates.none { it.host == host && it.port == port }) {
                            candidates.add(
                                ProxyNode(
                                    id = "pub-${candidates.size + 1}",
                                    name = "MTProto #${candidates.size + 1} ($host)",
                                    host = host,
                                    port = port,
                                    secret = secret,
                                    protocol = ProxyProtocol.MTPROTO
                                )
                            )
                        }
                    }

                    val jsonRegex = Regex(""""host"\s*:\s*"([^"]+)"\s*,\s*"port"\s*:\s*(\d+)\s*,\s*"secret"\s*:\s*"([^"]+)"""")
                    jsonRegex.findAll(body).take(20).forEach { m ->
                        val (host, portStr, secret) = m.destructured
                        val port = portStr.toIntOrNull() ?: 443
                        if (candidates.none { it.host == host && it.port == port }) {
                            candidates.add(
                                ProxyNode(

id = "pub-${candidates.size + 1}",
name = "MTProto #${candidates.size + 1} ($host)",
host = host,
port = port,
secret = secret,
protocol = ProxyProtocol.MTPROTO
)
)
}
}
}
if (candidates.size >= 10) break
}
if (candidates.isEmpty()) {
candidates.addAll(fallbackPublicNodes)
}
val checked = candidates.map { node ->
node.copy(pingMs = measureTcpPing(node.host, node.port))
}
val alive = checked.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
val resultList = if (alive.isNotEmpty()) alive else checked
_state.update { current ->
val merged = (resultList + current.nodes).distinctBy { "${it.host}:${it.port}" }
val first = merged.firstOrNull()
current.copy(
nodes = merged,
activeNodeId = first?.id ?: "",
enabled = merged.isNotEmpty(),
isCheckingPing = false,
statusNote = if (first != null && first.pingMs > 0) {
"Активен: ${first.host} (${first.pingMs} мс)"
} else {
"Загружено ${merged.size} узлов • добавьте свой tg://proxy для макс. скорости"
}
)
}
val active = _state.value.nodes.find { it.id == _state.value.activeNodeId }
RedFoxTdEngine.syncProxyState(_state.value.enabled, active)
}
}
fun addFromLink(rawLink: String): Boolean {
val clean = rawLink.trim()
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
name = "Свой узел ($host)",
host = host,
port = port,
secret = secret,
protocol = protocol,
pingMs = -1,
isBuiltIn = false
)
_state.update { current ->
current.copy(
nodes = listOf(newNode) + current.nodes,
activeNodeId = newNode.id,
enabled = true,
statusNote = "Подключён свой узел: $host:$port"
)
}
RedFoxTdEngine.syncProxyState(true, newNode)
refreshAllPings()
return true
}
}
