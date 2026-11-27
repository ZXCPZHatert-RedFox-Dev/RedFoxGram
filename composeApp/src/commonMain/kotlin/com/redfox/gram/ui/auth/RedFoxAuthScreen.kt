package com.redfox.gram.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.redfox.gram.auth.AuthStep
import com.redfox.gram.auth.RedFoxAuthManager
import com.redfox.gram.engine.RedFoxTdEngine
import com.redfox.gram.network.RedFoxProxyManager

@Composable
fun RedFoxAuthScreen(onOpenProxy: () -> Unit = {}) {
    val authState by RedFoxAuthManager.state.collectAsState()
    val proxyState by RedFoxProxyManager.state.collectAsState()
    val tdStatus by RedFoxTdEngine.connectionStatus.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                onClick = onOpenProxy,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val activeNode = proxyState.nodes.find { it.id == proxyState.activeNodeId }
                        Text(
                            text = if (proxyState.enabled && activeNode != null) {
                                "Прокси: ${activeNode.host}:${activeNode.port}"
                            } else {
                                "Соединение: Прямое (TDLib MTProto)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Прокси", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Статус ядра: $tdStatus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(targetState = authState.step) { step ->
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (step) {
                            AuthStep.PHONE -> {
                                Text("Вход в RedFoxGram", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Введите номер телефона. Код придёт в служебные уведомления Telegram на ваших устройствах.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = authState.phoneNumber,
                                    onValueChange = { RedFoxAuthManager.updatePhone(it) },
                                    label = { Text("Номер телефона") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { RedFoxAuthManager.submitPhone() },
                                    enabled = !authState.isLoading,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text(if (authState.isLoading) "Отправка запроса..." else "Получить код ->", fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { RedFoxAuthManager.completeLogin(skip2FA = true) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Быстрый предпросмотр UI (без входа)")
                                }
                            }

                            AuthStep.CODE -> {
                                Text("Код подтверждения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${authState.deliveryHint} (${authState.phoneNumber}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = authState.codeInput,
                                    onValueChange = { RedFoxAuthManager.updateCode(it) },
                                    label = { Text("5-значный код") },

singleLine = true,
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth()
)
Button(
onClick = { RedFoxAuthManager.verifyCode() },
enabled = !authState.isLoading,
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth().height(52.dp)
) {
Text("Подтвердить код", fontWeight = FontWeight.Bold)
}
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween
) {
TextButton(onClick = { RedFoxAuthManager.moveToStep(AuthStep.PHONE) }) {
Text("<- Изменить номер")
}
TextButton(onClick = { RedFoxTdEngine.resendCode() }) {
Text("Повторить код")
}
}
}
AuthStep.PASSWORD_2FA -> {
Text("Облачный пароль (2FA)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
Text(
"Введите ваш двухэтапный пароль от аккаунта Telegram.",
style = MaterialTheme.typography.bodyMedium,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
OutlinedTextField(
value = authState.password2FA,
onValueChange = { RedFoxAuthManager.updatePassword2FA(it) },
label = { Text("Пароль 2FA") },
visualTransformation = PasswordVisualTransformation(),
singleLine = true,
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth()
)
Button(
onClick = { RedFoxAuthManager.completeLogin(skip2FA = false) },
enabled = !authState.isLoading,
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth().height(52.dp)
) {
Text("Войти в чаты", fontWeight = FontWeight.Bold)
}
}
AuthStep.AUTHORIZED -> {}
}
authState.errorMessage?.let { err ->
Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
}
}
}
Text(
text = "RedFoxGram M3E • Open-Source Multi-Client Engine",
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.onSurfaceVariant,
modifier = Modifier.align(Alignment.CenterHorizontally)
)
}
}
}
