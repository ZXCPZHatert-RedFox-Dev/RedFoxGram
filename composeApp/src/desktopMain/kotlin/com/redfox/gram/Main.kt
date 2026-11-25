package com.redfox.gram

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RedFoxGram • Material 3 Expressive",
        state = rememberWindowState(width = 1040.dp, height = 740.dp)
    ) {
        RedFoxGramApp(isDesktop = true)
    }
}
