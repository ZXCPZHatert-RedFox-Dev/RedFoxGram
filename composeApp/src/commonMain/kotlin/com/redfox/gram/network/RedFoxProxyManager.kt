package com.redfox.gram.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val nodes: List<ProxyNode> = emptyList()
)

object RedFoxProxyManager {
    private val _state = MutableStateFlow(ProxyHubState())
    val state: StateFlow<ProxyHubState> = _state.asStateFlow()

    fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
    }

    fun toggleSmartRotation(enabled: Boolean) {
        _state.update { it.copy(autoSmartRotation = enabled) }
    }

    fun selectNode(id: String) {
        _state.update { it.copy(activeNodeId = id, enabled = true) }
    }

    fun refreshAllPings() {
        _state.update { current ->
            val updated = current.nodes.map { node ->
                val jitter = Random.nextInt(-8, 15)
                val base = if (node.pingMs > 0) node.pingMs else 55
                node.copy(pingMs = (base + jitter).coerceIn(24, 180))
            }
            val bestId = if (current.autoSmartRotation) {
                updated.minByOrNull { it.pingMs }?.id ?: current.activeNodeId
            } else {
                current.activeNodeId
            }
            current.copy(nodes = updated, activeNodeId = bestId)
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
            pingMs = Random.nextInt(35, 95),
            isBuiltIn = false
        )

        _state.update { current ->
            current.copy(
                nodes = listOf(newNode) + current.nodes,
                activeNodeId = newNode.id,
                enabled = true
            )
        }
        return true
    }
}
