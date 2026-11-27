package com.redfox.gram
import androidx.compose.runtime.*
import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.ui.auth.RedFoxAuthScreen
import com.redfox.gram.ui.home.RedFoxMainScreen
import com.redfox.gram.ui.proxy.RedFoxProxyScreen
import com.redfox.gram.ui.theme.RedFoxM3ETheme
@Composable
fun App() {
RedFoxM3ETheme {
val authState by RedFoxAuthManager.state.collectAsState()
var showProxyFromAuth by remember { mutableStateOf(false) }
if (showProxyFromAuth) {
RedFoxProxyScreen(onBack = { showProxyFromAuth = false })
} else if (authState.step != AuthStep.AUTHORIZED) {
RedFoxAuthScreen(onOpenProxy = { showProxyFromAuth = true })
} else {
RedFoxMainScreen()
}
}
}
