package com.redfox.gram.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AuthStep {
    PHONE,
    CODE,
    PASSWORD_2FA,
    AUTHORIZED
}

data class AuthUiState(
    val step: AuthStep = AuthStep.PHONE,
    val phoneNumber: String = "+7 ",
    val codeInput: String = "",
    val password2FA: String = "",
    val accountName: String = "RedFox User",
    val errorMessage: String? = null
)

object RedFoxAuthManager {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updatePhone(value: String) {
        _state.update { it.copy(phoneNumber = value, errorMessage = null) }
    }

    fun submitPhone() {
        val digits = _state.value.phoneNumber.filter { it.isDigit() }
        if (digits.length < 10) {
            _state.update { it.copy(errorMessage = "Введите корректный номер телефона") }
            return
        }
        _state.update { it.copy(step = AuthStep.CODE, errorMessage = null) }
    }

    fun updateCode(value: String) {
        val filtered = value.filter { it.isDigit() }.take(5)
        _state.update { it.copy(codeInput = filtered, errorMessage = null) }
        if (filtered.length == 5) {
            verifyCode()
        }
    }

    fun verifyCode() {
        if (_state.value.codeInput.length < 5) {
            _state.update { it.copy(errorMessage = "Введите 5-значный код из Telegram") }
            return
        }
        _state.update { it.copy(step = AuthStep.PASSWORD_2FA, errorMessage = null) }
    }

    fun updatePassword2FA(value: String) {
        _state.update { it.copy(password2FA = value, errorMessage = null) }
    }

    fun completeLogin(skip2FA: Boolean = false) {
        if (!skip2FA && _state.value.password2FA.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите облачный пароль или нажмите «Пропустить»") }
            return
        }
        _state.update { it.copy(step = AuthStep.AUTHORIZED, errorMessage = null) }
    }

    fun logout() {
        _state.value = AuthUiState(step = AuthStep.PHONE)
    }
}
