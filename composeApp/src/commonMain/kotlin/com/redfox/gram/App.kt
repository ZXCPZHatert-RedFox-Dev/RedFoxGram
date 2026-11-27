package com.redfox.gram

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.redfox.gram.ui.home.RedFoxMainScreen

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        RedFoxMainScreen()
    }
}
