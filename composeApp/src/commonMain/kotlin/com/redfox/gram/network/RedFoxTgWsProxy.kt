package com.redfox.gram.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

/**
 * Встроенный движок tg-ws-proxy:
 * Поднимает локальный SOCKS5-мост на 127.0.0.1:10809 и туннелирует трафик TDLib
 * в защищённый WebSocket (wss://kws*.web.telegram.org/apiws) в обход блокировок MTProto.
 */
object RedFoxTgWsProxy {
    const val LOCAL_PORT = 10809
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(): Boolean {
        if (isRunning) return true
        return runCatching {
            val srv = ServerSocket(LOCAL_PORT, 64, InetAddress.getByName("127.0.0.1"))
            serverSocket = srv
            isRunning = true
            scope.launch {
                while (isActive && isRunning) {
                    val client = runCatching { srv.accept() }.getOrNull() ?: break
                    thread(isDaemon = true) {
                        handleSocks5Client(client)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handleSocks5Client(client: Socket) {
        try {
            client.tcpNoDelay = true
            val cIn = client.getInputStream()
            val cOut = client.getOutputStream()

            // 1. SOCKS5 Greeting
            val ver = cIn.read()
            if (ver != 0x05) {
                client.close()
                return
            }
            val nMethods = cIn.read()
            repeat(nMethods) { cIn.read() }
            cOut.write(byteArrayOf(0x05, 0x00))
            cOut.flush()

            // 2. SOCKS5 CONNECT Request
            val reqVer = cIn.read()
            val cmd = cIn.read()
            cIn.read() // RSV
            val atyp = cIn.read()
            if (reqVer != 0x05 || cmd != 0x01) {
                client.close()
                return
            }

            val targetHost = when (atyp) {
                0x01 -> {
                    val ip = ByteArray(4)
                    readFully(cIn, ip)
                    "${ip[0].toInt() and 0xFF}.${ip[1].toInt() and 0xFF}.${ip[2].toInt() and 0xFF}.${ip[3].toInt() and 0xFF}"
                }
                0x03 -> {
                    val len = cIn.read()
                    val domain = ByteArray(len)
                    readFully(cIn, domain)
                    String(domain, Charsets.UTF_8)
                }
                0x04 -> {
                    val ip6 = ByteArray(16)
                    readFully(cIn, ip6)
                    "149.154.167.50"
                }
                else -> {
                    client.close()
                    return
                }
            }
            val portHi = cIn.read()
            val portLo = cIn.read()
            val targetPort = (portHi shl 8) or portLo

            // Маппинг DC Telegram на соответствующие WebSocket-шлюзы kws1..kws5.web.telegram.org
            val wsHost = mapDcToWsGateway(targetHost)

            // 3. Устанавливаем WSS-соединение (TLS + HTTP Upgrade WebSocket binary)
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val tlsSocket = sslFactory.createSocket(wsHost, 443) as SSLSocket
            tlsSocket.tcpNoDelay = true
            val sslParams = SSLParameters()
            sslParams.serverNames = listOf(SNIHostName(wsHost))
            tlsSocket.sslParameters = sslParams
            tlsSocket.startHandshake()

            val wsIn = tlsSocket.getInputStream()
            val wsOut = tlsSocket.getOutputStream()

            val wsKeyBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val wsKey = Base64.getEncoder().encodeToString(wsKeyBytes)
            val upgradeReq = buildString {
                append("GET /apiws HTTP/1.1\r\n")
                append("Host: $wsHost\r\n")
                append("Upgrade: websocket\r\n")
                append("Connection: Upgrade\r\n")
                append("Sec-WebSocket-Key: $wsKey\r\n")
                append("Sec-WebSocket-Version: 13\r\n")
                append("Sec-WebSocket-Protocol: binary\r\n")
                append("Origin: https://telegram.org\r\n")
                append("User-Agent: Mozilla/5.0 (Linux; Android 17) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36\r\n\r\n")
            }
            wsOut.write(upgradeReq.toByteArray(Charsets.UTF_8))
            wsOut.flush()

            // Читаем HTTP 101 Switching Protocols
            val headerBuf = StringBuilder()
            while (!headerBuf.endsWith("\r\n\r\n")) {
                val b = wsIn.read()
                if (b == -1) break
                headerBuf.append(b.toChar())
            }
            if (!headerBuf.toString().contains("101")) {
                tlsSocket.close()
                client.close()
                return
            }

            // Отправляем SOCKS5 Success клиенту TDLib
            cOut.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            cOut.flush()

            // 4. Двусторонний мост: TDLib TCP <-> Masked WebSocket Binary Frames
            val t1 = thread(isDaemon = true) {
                try {
                    val buf = ByteArray(16384)
                    val rng = SecureRandom()
                    while (true) {
                        val n = cIn.read(buf)
                        if (n <= 0) break
                        sendWsBinaryFrame(wsOut, buf, n, rng)
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { tlsSocket.close() }
                    runCatching { client.close() }
                }
            }

            try {
                while (true) {
                    val payload = readWsFrame(wsIn) ?: break
                    if (payload.isNotEmpty()) {
                        cOut.write(payload)
                        cOut.flush()
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { client.close() }
                runCatching { tlsSocket.close() }
                t1.interrupt()
            }
        } catch (_: Exception) {
            runCatching { client.close() }
        }
    }

    private fun mapDcToWsGateway(ipOrHost: String): String {
        return when {
            ipOrHost.startsWith("149.154.175.") -> "kws1.web.telegram.org"
            ipOrHost.startsWith("149.154.167.") -> "kws2.web.telegram.org"
            ipOrHost.startsWith("149.154.174.") -> "kws3.web.telegram.org"
            ipOrHost.startsWith("149.154.165.") || ipOrHost.startsWith("91.108.4.") -> "kws4.web.telegram.org"
            ipOrHost.startsWith("91.108.56.") -> "kws5.web.telegram.org"
            else -> "kws2.web.telegram.org"
        }
    }

    private fun sendWsBinaryFrame(out: OutputStream, data: ByteArray, length: Int, rng: SecureRandom) {
        synchronized(out) {
            out.write(0x82) // FIN + Binary opcode
            when {
                length <= 125 -> out.write(0x80 or length)
                length <= 65535 -> {
                    out.write(0x80 or 126)
                    out.write((length shr 8) and 0xFF)
                    out.write(length and 0xFF)
                }
                else -> {
                    out.write(0x80 or 127)
                    val lenLong = length.toLong()
                    for (i in 7 downTo 0) out.write(((lenLong shr (i * 8)) and 0xFF).toInt())
                }
            }
            val mask = ByteArray(4).also { rng.nextBytes(it) }
            out.write(mask)
            val masked = ByteArray(length)
            for (i in 0 until length) {
                masked[i] = (data[i].toInt() xor mask[i and 3].toInt()).toByte()
            }
            out.write(masked)
            out.flush()
        }
    }

    private fun readWsFrame(input: InputStream): ByteArray? {
        val b1 = input.read()
        if (b1 == -1) return null
        val opcode = b1 and 0x0F
        val b2 = input.read()
        if (b2 == -1) return null
        val masked = (b2 and 0x80) != 0
        var len = (b2 and 0x7F).toLong()
        if (len == 126L) {
            len = ((input.read() shl 8) or input.read()).toLong()
        } else if (len == 127L) {
            len = 0L
            repeat(8) { len = (len shl 8) or (input.read().toLong() and 0xFF) }
        }
        val mask = if (masked) ByteArray(4).also { readFully(input, it) } else null
        if (opcode == 0x08) return null // Close frame
        val payload = ByteArray(len.toInt())
        readFully(input, payload)
        if (mask != null) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor mask[i and 3].toInt()).toByte()
            }
        }
        return if (opcode == 0x02 || opcode == 0x01) payload else ByteArray(0)
    }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val r = input.read(buf, off, buf.size - off)

if (r <= 0) throw java.io.EOFException()
off += r
}
}
}
