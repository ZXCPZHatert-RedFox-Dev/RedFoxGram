package com.redfox.gram

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RedFoxGram M3E"
    ) {
        App()
    }
}
