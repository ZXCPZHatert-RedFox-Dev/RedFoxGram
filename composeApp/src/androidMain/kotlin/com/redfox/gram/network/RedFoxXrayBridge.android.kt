package com.redfox.gram.network

import android.app.Application
import android.content.Context
import java.io.File
import java.net.URI
import java.net.URLDecoder

actual object RedFoxXrayBridge {
    private var xrayProcess: Process? = null

    private fun resolveAppContext(): Context? {
        return runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getMethod("currentApplication")
            method.invoke(null) as? Application
        }.getOrNull()
    }

    actual fun startVpnUri(uri: String, localSocksPort: Int): Boolean {
        stop()
        val ctx = resolveAppContext() ?: return false
        val nativeDir = ctx.applicationInfo.nativeLibraryDir
        val xrayBin = File(nativeDir, "libxray.so")
        if (!xrayBin.exists()) return false

        val configJson = buildXrayConfigJson(uri, localSocksPort) ?: return false
        val cfgFile = File(ctx.filesDir, "redfox_xray_config.json")
        cfgFile.writeText(configJson)

        return runCatching {
            val pb = ProcessBuilder(xrayBin.absolutePath, "run", "-c", cfgFile.absolutePath)
            pb.directory(ctx.filesDir)
            pb.redirectErrorStream(true)
            xrayProcess = pb.start()
            true
        }.getOrDefault(false)
    }

    actual fun stop() {
        runCatching { xrayProcess?.destroy() }
        xrayProcess = null
    }

    private fun buildXrayConfigJson(rawUri: String, socksPort: Int): String? {
        val clean = rawUri.trim()
        return when {
            clean.startsWith("vless://", ignoreCase = true) -> {
                val u = URI(clean)
                val uuid = u.userInfo ?: return null
                val host = u.host ?: return null
                val port = if (u.port > 0) u.port else 443
                val params = (u.rawQuery ?: "").split("&").mapNotNull {
                    val idx = it.indexOf('=')
                    if (idx > 0) it.substring(0, idx) to URLDecoder.decode(it.substring(idx + 1), "UTF-8") else null
                }.toMap()
                val security = params["security"] ?: "none"
                val sni = params["sni"] ?: host
                val pbk = params["pbk"] ?: ""
                val sid = params["sid"] ?: ""
                val fp = params["fp"] ?: "chrome"
                val netType = params["type"] ?: "tcp"
                val wsPath = params["path"] ?: "/"

                val tlsSection = when (security) {
                    "reality" -> """"security":"reality","realitySettings":{"show":false,"fingerprint":"$fp","serverName":"$sni","publicKey":"$pbk","shortId":"$sid"}"""
                    "tls" -> """"security":"tls","tlsSettings":{"serverName":"$sni","fingerprint":"$fp"}"""
                    else -> """"security":"none""""
                }
                val netSection = when (netType) {
                    "ws" -> """"network":"ws","wsSettings":{"path":"$wsPath","headers":{"Host":"$sni"}}"""
                    "grpc" -> """"network":"grpc","grpcSettings":{"serviceName":"${params["serviceName"] ?: ""}"}"""
                    else -> """"network":"tcp""""
                }

                """
                {
                  "log": {"loglevel": "warning"},
                  "inbounds": [{"port": $socksPort, "listen": "127.0.0.1", "protocol": "socks", "settings": {"udp": true}}],
                  "outbounds": [{
                    "protocol": "vless",
                    "settings": {"vnext": [{"address": "$host", "port": $port, "users": [{"id": "$uuid", "encryption": "none"}]}]},
                    "streamSettings": {$netSection, $tlsSection}
                  }]
                }
                """.trimIndent()
            }
            clean.startsWith("trojan://", ignoreCase = true) -> {
                val u = URI(clean)
                val password = u.userInfo ?: return null
                val host = u.host ?: return null
                val port = if (u.port > 0) u.port else 443
                """
                {
                  "log": {"loglevel": "warning"},
                  "inbounds": [{"port": $socksPort, "listen": "127.0.0.1", "protocol": "socks", "settings": {"udp": true}}],
                  "outbounds": [{
                    "protocol": "trojan",
                    "settings": {"servers": [{"address": "$host", "port": $port, "password": "$password"}]},
                    "streamSettings": {"network": "tcp", "security": "tls", "tlsSettings": {"serverName": "$host"}}
                  }]
                }
                """.trimIndent()
            }
            else -> null
        }
    }
}
