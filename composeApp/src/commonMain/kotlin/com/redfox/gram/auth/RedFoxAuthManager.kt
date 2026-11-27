package com.redfox.gram.auth

import com.redfox.gram.engine.RedFoxTdEngine
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
    val errorMessage: String? = null,
    val deliveryHint: String = "Код отправлен в чат Telegram на ваши другие устройства",
    val isLoading: Boolean = false
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
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        RedFoxTdEngine.sendPhoneNumber(_state.value.phoneNumber)
    }

    fun moveToStep(step: AuthStep) {
        _state.update { it.copy(step = step, isLoading = false, errorMessage = null) }
    }

    fun setDeliveryHint(hint: String) {
        _state.update { it.copy(deliveryHint = hint) }
    }

    fun setAuthError(message: String) {
        _state.update { it.copy(isLoading = false, errorMessage = message) }
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
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        RedFoxTdEngine.sendAuthCode(_state.value.codeInput)
    }

    fun updatePassword2FA(value: String) {
        _state.update { it.copy(password2FA = value, errorMessage = null) }
    }

    fun completeLogin(skip2FA: Boolean = false) {
        if (skip2FA) {
            _state.update { it.copy(step = AuthStep.AUTHORIZED, isLoading = false, errorMessage = null) }
            return
        }
        if (_state.value.password2FA.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите облачный пароль 2FA") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        RedFoxTdEngine.sendPassword2FA(_state.value.password2FA)
    }

    fun logout() {
        RedFoxTdEngine.logOut()
        _state.value = AuthUiState(step = AuthStep.PHONE)
    }
}
