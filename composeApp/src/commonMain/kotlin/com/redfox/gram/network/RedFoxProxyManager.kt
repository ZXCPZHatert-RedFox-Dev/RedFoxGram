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
import java.io.File
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
    val statusNote: String = "Вставьте ссылку tg://proxy или нажмите «Проверить пинг»",
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()
    private val storageFile = File(System.getProperty("java.io.tmpdir"), "redfox_saved_proxies.txt")

    init {
        loadSavedNodes()
    }

    private fun loadSavedNodes() {
        runCatching {
            if (storageFile.exists()) {
                val saved = storageFile.readLines().mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) {
                        ProxyNode(
                            id = parts[0],
                            name = parts[1],
                            host = parts[2],
                            port = parts[3].toIntOrNull() ?: 443,
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
            val lines = nodes.filter { !it.isBuiltIn }.map {
                "${it.id}|${it.name}|${it.host}|${it.port}|${it.secret}"
            }
            storageFile.writeText(lines.joinToString("\n"))
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        if (!enabled) {
            _state.update { it.copy(statusNote = "Прямое соединение") }
            RedFoxTdEngine.syncProxyState(false, null)
            return
        }
        val custom = _state.value.nodes.filter { !it.isBuiltIn }
        if (custom.isNotEmpty()) {
            val first = custom.first()
            _state.update { it.copy(activeNodeId = first.id, statusNote = "Подключено: ${first.host}:${first.port}") }
            RedFoxTdEngine.syncProxyState(true, first)
        } else {
            fetchPublicMtprotoNodes()
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
                statusNote = if (node != null) "Проверка узла ${node.host}..." else "Узел выбран"
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
        fetchPublicMtprotoNodes()
    }

    fun fetchPublicMtprotoNodes() {
        _state.update { it.copy(isCheckingPing = true, statusNote = "Поиск доступных MTProto-прокси...") }
        scope.launch {
            val candidates = mutableListOf<ProxyNode>()
            val sources = listOf(
                "https://mtpro.xyz",
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
                    linkRegex.findAll(body).take(25).forEach { m ->
                        val (host, portStr, secret) = m.destructured
                        val port = portStr.toIntOrNull() ?: 443
                        if (candidates.none { it.host == host && it.port == port }) {
                            candidates.add(ProxyNode("pub-${candidates.size + 1}", "MTProto #${candidates.size + 1} ($host)", host, port, secret, ProxyProtocol.MTPROTO))
                        }
                    }
                    val jsonRegex = Regex(""""host"\s*:\s*"([^"]+)"\s*,\s*"port"\s*:\s*(\d+)\s*,\s*"secret"\s*:\s*"([^"]+)"""")
                    jsonRegex.findAll(body).take(25).forEach { m ->
                        val (host, portStr, secret) = m.destructured
                        val port = portStr.toIntOrNull() ?: 443
                        if (candidates.none { it.host == host && it.port == port }) {
                            candidates.add(ProxyNode("pub-${candidates.size + 1}", "MTProto #${candidates.size + 1} ($host)", host, port, secret, ProxyProtocol.MTPROTO))
                        }
                    }
                }
                if (candidates.size >= 15) break
            }

            val customNodes = _state.value.nodes.filter { !it.isBuiltIn }
            val allToTest = (customNodes + candidates).distinctBy { "${it.host}:${it.port}" }

            val checked = allToTest.map { node ->
                val tdMs = RedFoxTdEngine.testProxyWithTdlib(node)
                val finalMs = if (tdMs > 0) tdMs else measureTcpPing(node.host, node.port)
                node.copy(pingMs = finalMs)
            }

            val alive = checked.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
            val sortedAll = alive + checked.filter { it.pingMs <= 0 }
            val best = alive.firstOrNull()

            _state.update { current ->
                current.copy(
                    nodes = sortedAll,
                    activeNodeId = best?.id ?: current.activeNodeId,
                    enabled = best != null,
                    isCheckingPing = false,
                    statusNote = if (best != null) {
                        "Активен: ${best.host} (${best.pingMs} мс)"
                    } else {
                        "Авто-списки заблокированы провайдером — вставьте ссылку tg://proxy ниже"
                    }
                )
            }
            RedFoxTdEngine.syncProxyState(best != null, best)
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
            name = "Свой прокси ($host)",
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
                activeNodeId = newNode.id,
                enabled = true,
                statusNote = "Подключение к $host:$port..."
            )
        }
        scope.launch {
            val ms = RedFoxTdEngine.testProxyWithTdlib(newNode).let { if (it > 0) it else measureTcpPing(host, port) }
            _state.update { current ->
                current.copy(

nodes = current.nodes.map { if (it.id == newNode.id) it.copy(pingMs = ms) else it },
statusNote = if (ms > 0) "Подключено: $host ($ms мс)" else "Узел $host добавлен (проверьте связь)"
)
}
RedFoxTdEngine.syncProxyState(true, newNode.copy(pingMs = ms))
}
return true
}
}
