package fintrack.proyecto4.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Locked(val minutesRemaining: Long) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signIn(email: String, password: String, rememberMe: Boolean) {
        if (email.isBlank() || password.isBlank()) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _uiState.value = when (val result = authRepository.signIn(email.trim().lowercase(), password, rememberMe)) {
                is LoginResult.Success -> LoginUiState.Success
                is LoginResult.Error -> LoginUiState.Error(result.message)
                is LoginResult.AccountLocked -> LoginUiState.Locked(result.minutesRemaining)
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
