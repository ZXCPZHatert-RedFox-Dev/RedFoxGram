package com.redfox.gram.network

actual object RedFoxXrayBridge {
    actual fun startVpnUri(uri: String, localSocksPort: Int): Boolean = false
    actual fun stop() {}
}
