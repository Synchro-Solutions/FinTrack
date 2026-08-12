package fintrack.proyecto4.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val PASSWORD_RESET_COOLDOWN_SECONDS = 60

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun isValidEmailFormat(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    object Sent : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds

    private var cooldownJob: Job? = null

    fun sendResetLink(email: String) {
        val trimmedEmail = email.trim()

        if (!isValidEmailFormat(trimmedEmail)) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa un correo electrónico válido")
            return
        }
        if (_cooldownSeconds.value > 0 || _uiState.value is ForgotPasswordUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading
            when (val result = authRepository.sendPasswordResetEmail(trimmedEmail.lowercase())) {
                is PasswordResetResult.Success -> {
                    _uiState.value = ForgotPasswordUiState.Sent
                    startCooldown()
                }
                is PasswordResetResult.NetworkError -> {
                    _uiState.value = ForgotPasswordUiState.Error(result.message)
                }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _cooldownSeconds.value = PASSWORD_RESET_COOLDOWN_SECONDS
            while (_cooldownSeconds.value > 0) {
                delay(1_000)
                _cooldownSeconds.value -= 1
            }
        }
    }

    fun clearError() {
        if (_uiState.value is ForgotPasswordUiState.Error) {
            _uiState.value = ForgotPasswordUiState.Idle
        }
    }
}