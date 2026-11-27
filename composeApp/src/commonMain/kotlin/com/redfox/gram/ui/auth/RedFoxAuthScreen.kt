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
import com.redfox.gram.network.RedFoxProxyManager

@Composable
fun RedFoxAuthScreen(onOpenProxy: () -> Unit = {}) {
    val authState by RedFoxAuthManager.state.collectAsState()
    val proxyState by RedFoxProxyManager.state.collectAsState()

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
            // Верхняя плашка статуса встроенного обхода RedFox Shield
            Card(
                onClick = onOpenProxy,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeNode = proxyState.nodes.find { it.id == proxyState.activeNodeId }
                    Text(
                        text = if (proxyState.enabled) {
                            "RedFox Shield: ${activeNode?.name ?: "MTProxy"} (${activeNode?.pingMs ?: 42} мс)"
                        } else {
                            "RedFox Shield: прямое соединение"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Настроить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Центральная карточка авторизации M3E
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
                                    "Введите номер телефона аккаунта Telegram. Соединение защищено MTProto Fake-TLS.",
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
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Получить код ->", fontWeight = FontWeight.Bold)
                                }
                            }

                            AuthStep.CODE -> {
                                Text("Код подтверждения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Мы отправили 5-значный код в официальное приложение Telegram на номер ${authState.phoneNumber}.",
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
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Подтвердить код", fontWeight = FontWeight.Bold)
                                }
                            }

                            AuthStep.PASSWORD_2FA -> {
                                Text("Облачный пароль (2FA)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Если на аккаунте включена двухэтапная аутентификация, введите пароль.",
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
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth().height(52.dp)
) {
Text("Войти в чаты", fontWeight = FontWeight.Bold)
}
TextButton(
onClick = { RedFoxAuthManager.completeLogin(skip2FA = true) },
modifier = Modifier.fillMaxWidth()
) {
Text("У меня нет облачного пароля (Пропустить)")
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
