package fintrack.proyecto4.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun register(email: String, password: String, confirm: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error("Todos los campos son obligatorios")
            return
        }

        if (password != confirm) {
            _uiState.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }

        val validation = validatePassword(password)
        if (validation != null) {
            _uiState.value = RegisterUiState.Error(validation)
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            _uiState.value = when (val result = authRepository.register(email.trim().lowercase(), password)) {
                is RegisterResult.Success -> RegisterUiState.Success
                is RegisterResult.Error -> RegisterUiState.Error(result.message)
            }
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) return "Mínimo 8 caracteres"
        if (!password.any { it.isUpperCase() }) return "Debe incluir una mayúscula"
        if (!password.any { it.isDigit() }) return "Debe incluir un número"
        return null
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
