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
    val statusNote: String = "Нажмите тумблер для авто-поиска живых MTProto-прокси",
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()

    fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        if (!enabled) {
            _state.update { it.copy(statusNote = "Прямое соединение") }
            RedFoxTdEngine.syncProxyState(false, null)
            return
        }
        fetchPublicMtprotoNodes()
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
                socket.connect(InetSocketAddress(host, port), 2200)
            }
            (System.currentTimeMillis() - start).toInt().coerceAtLeast(1)
        }.getOrDefault(-1)
    }

    fun refreshAllPings() {
        // Всегда подтягиваем свежий пул прокси + перепроверяем существующие
        fetchPublicMtprotoNodes()
    }

    fun fetchPublicMtprotoNodes() {
        _state.update { it.copy(isCheckingPing = true, statusNote = "Скачивание свежих MTProto-прокси...") }
        scope.launch {
            val candidates = mutableListOf<ProxyNode>()
            val sources = listOf(
                "https://mtpro.xyz",
                "https://githubusercontent.com",
                "https://jsdelivr.net",
                "https://jsdelivr.net",
                "https://jsdelivr.net"
            )
            for (src in sources) {
                runCatching {
                    val conn = URL(src).openConnection().apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android 17)")
                    }
                    val body = conn.getInputStream().bufferedReader().readText()

                    // 1. Формат ссылок tg://proxy?server=...&port=...&secret=...
                    val linkRegex = Regex("""server=([a-zA-Z0-9.\-]+)&port=(\d+)&secret=([a-zA-Z0-9_\-=]+)""")
                    linkRegex.findAll(body).take(30).forEach { m ->
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

                    // 2. Формат JSON mtpro.xyz / hookzof ("host":"...","port":...,"secret":"...")
                    val jsonRegex = Regex(""""host"\s*:\s*"([^"]+)"\s*,\s*"port"\s*:\s*(\d+)\s*,\s*"secret"\s*:\s*"([^"]+)"""")
                    jsonRegex.findAll(body).take(30).forEach { m ->
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
                if (candidates.size >= 20) break
            }

            // Сохраняем пользовательские добавленные ссылки + свежие кандидаты
            val customNodes = _state.value.nodes.filter { !it.isBuiltIn }
            val allToTest = (customNodes + candidates).distinctBy { "${it.host}:${it.port}" }

            _state.update { it.copy(statusNote = "Проверка пинга ${allToTest.size} серверов...") }

            val checked = allToTest.map { node ->
                val tcpMs = measureTcpPing(node.host, node.port)
                val finalMs = if (tcpMs > 0) {
                    val tdMs = RedFoxTdEngine.testProxyWithTdlib(node)
                    if (tdMs > 0) tdMs else tcpMs
                } else -1
                node.copy(pingMs = finalMs)
            }

            val alive = checked.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
            val sortedAll = alive + checked.filter { it.pingMs <= 0 }
            val best = alive.firstOrNull()

            _state.update { current ->
                current.copy(
                    nodes = sortedAll,
                    activeNodeId = best?.id ?: "",
                    enabled = best != null,
                    isCheckingPing = false,
                    statusNote = if (best != null) {
                        "Подключено: ${best.host} (${best.pingMs} мс)"
                    } else if (sortedAll.isEmpty()) {
                        "Нет доступа к зеркалам — вставьте ссылку tg://proxy вручную"
                    } else {
                        "Живых серверов не найдено (0/${sortedAll.size}) — вставьте свой tg://proxy"
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
                statusNote = "Проверка своего узла $host:$port..."
            )
        }
        scope.launch {
            val ms = RedFoxTdEngine.testProxyWithTdlib(newNode).let { if (it > 0) it else measureTcpPing(host, port) }
            _state.update { current ->
                current.copy(
                    nodes = current.nodes.map { if (it.id == newNode.id) it.copy(pingMs = ms) else it },
                    statusNote = if (ms > 0) "Подключено: $host ($ms мс)" else "Узел $host не отвечает (-1 мс)"
                )
            }
            if (ms > 0) {
                RedFoxTdEngine.syncProxyState(true, newNode.copy(pingMs = ms))
            }
        }
        return true
    }
}
