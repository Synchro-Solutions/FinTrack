package fintrack.proyecto4.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    data object Idle : RegisterUiState()
    data object Loading : RegisterUiState()
    data object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

/**
 * Registro con email y contraseña (US-01). Crea la cuenta en Firebase mediante
 * [AuthClient.registerWithEmail] y, best-effort, persiste la sesión igual que el login para
 * que el usuario quede autenticado y pase directo al onboarding.
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun register(email: String, password: String, confirmPassword: String) {
        val trimmed = email.trim().lowercase()
        when {
            trimmed.isBlank() -> {
                _uiState.value = RegisterUiState.Error("Ingresa tu correo electrónico")
                return
            }
            password.length < 6 -> {
                _uiState.value = RegisterUiState.Error("La contraseña debe tener al menos 6 caracteres")
                return
            }
            password != confirmPassword -> {
                _uiState.value = RegisterUiState.Error("Las contraseñas no coinciden")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            _uiState.value = AuthClient.registerWithEmail(trimmed, password).fold(
                onSuccess = {
                    // Firebase ya deja al usuario autenticado tras crear la cuenta; se persiste
                    // la sesión (rememberMe) para que sobreviva a reinicios, igual que el login.
                    runCatching { authRepository.signIn(trimmed, password, rememberMe = true) }
                    RegisterUiState.Success
                },
                onFailure = { RegisterUiState.Error(mapError(it.message)) }
            )
        }
    }

    private fun mapError(message: String?): String = when {
        message == null -> "No se pudo crear la cuenta. Intenta de nuevo."
        message.contains("already", ignoreCase = true) ||
            message.contains("EMAIL_EXISTS", ignoreCase = true) -> "Ese correo ya está registrado"
        message.contains("badly formatted", ignoreCase = true) ||
            message.contains("INVALID_EMAIL", ignoreCase = true) -> "Correo electrónico inválido"
        message.contains("network", ignoreCase = true) -> "Sin conexión. Revisa tu internet."
        else -> message
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
