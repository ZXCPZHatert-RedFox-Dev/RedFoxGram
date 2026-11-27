package com.redfox.gram.network

expect object RedFoxXrayBridge {
    fun startVpnUri(uri: String, localSocksPort: Int = 10808): Boolean
    fun stop()
}
