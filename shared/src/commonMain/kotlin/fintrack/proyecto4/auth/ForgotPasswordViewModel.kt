package fintrack.proyecto4.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val PASSWORD_RESET_COOLDOWN_SECONDS = 60

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

sealed class ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState()
    data object Loading : ForgotPasswordUiState()

    /** El correo de recuperación ya se solicitó; [secondsRemaining] cuenta hacia 0. */
    data class Sent(val secondsRemaining: Int) : ForgotPasswordUiState()

    /** Validación local (email vacío/inválido) o error de conexión real. */
    data class Error(val message: String) : ForgotPasswordUiState()
}

/**
 * Nunca distingue "usuario no existe" de un envío real: [AuthRepository.sendPasswordResetEmail]
 * ya colapsa ambos casos en [PasswordResetResult.Success] (ver AuthRepository.kt), así que aquí
 * ambos terminan mostrando el mismo mensaje de confirmación genérico.
 */
class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    private var cooldownJob: Job? = null

    fun sendResetLink(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa tu correo electrónico")
            return
        }
        if (!EMAIL_REGEX.matches(trimmed)) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa un correo electrónico válido")
            return
        }

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading
            when (authRepository.sendPasswordResetEmail(trimmed.lowercase())) {
                is PasswordResetResult.Success -> startCooldown()
                is PasswordResetResult.ConnectionError -> {
                    _uiState.value = ForgotPasswordUiState.Error("Error de conexión. Intenta de nuevo.")
                }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in PASSWORD_RESET_COOLDOWN_SECONDS downTo 0) {
                _uiState.value = ForgotPasswordUiState.Sent(remaining)
                if (remaining > 0) delay(1000)
            }
        }
    }

    /** Limpia un error de validación/conexión cuando el usuario retoma la edición del campo. */
    fun clearError() {
        if (_uiState.value is ForgotPasswordUiState.Error) {
            _uiState.value = ForgotPasswordUiState.Idle
        }
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }
}
