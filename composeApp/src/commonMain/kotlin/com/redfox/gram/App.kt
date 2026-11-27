package com.redfox.gram

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.config.RedFoxConfig
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.ui.auth.RedFoxAuthScreen
import com.redfox.gram.ui.home.RedFoxMainScreen
import com.redfox.gram.ui.proxy.RedFoxProxyScreen
import com.redfox.gram.ui.splash.LaunchAnimationStyle
import com.redfox.gram.ui.splash.RedFoxSplashOverlay
import com.redfox.gram.ui.theme.RedFoxM3ETheme

@Composable
fun App(androidFilesDir: String? = null) {
    LaunchedEffect(Unit) {
        val dbPath = if (androidFilesDir != null) "$androidFilesDir/redfox_tdlib" else "redfox_tdlib_db"
        RedFoxTdEngine.start(dbDirectory = dbPath)
    }

    RedFoxM3ETheme {
        val authState by RedFoxAuthManager.state.collectAsState()
        val settings by RedFoxConfig.state.collectAsState()
        var showProxyFromAuth by remember { mutableStateOf(false) }
        var splashDone by remember { mutableStateOf(false) }

        val launchStyle = if (settings.springPhysicsScroll) {
            LaunchAnimationStyle.M3E_FLOWER_FOX
        } else {
            LaunchAnimationStyle.EXPRESSIVE_BURST
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showProxyFromAuth) {
                RedFoxProxyScreen(onBack = { showProxyFromAuth = false })
            } else if (authState.step != AuthStep.AUTHORIZED) {
                RedFoxAuthScreen(onOpenProxy = { showProxyFromAuth = true })
            } else {
                RedFoxMainScreen()
            }

            if (!splashDone) {
                RedFoxSplashOverlay(
                    style = launchStyle,
                    onFinished = { splashDone = true }
                )
            }
        }
    }
}
