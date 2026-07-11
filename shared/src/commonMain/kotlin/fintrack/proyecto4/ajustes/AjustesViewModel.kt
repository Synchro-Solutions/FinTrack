package fintrack.proyecto4.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AjustesUiState(
    val isLoading: Boolean = true,
    val nombre: String = "",
    val email: String = "",
    val moneda: String = "CRC (₡)",
    val ingresoMensual: Double = 0.0
)

class AjustesViewModel(
    private val onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    private val uid: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(AjustesUiState())
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = runCatching { onboardingRepository.getProfile(uid) }.getOrNull()
            val email = AuthClient.currentUserEmail() ?: ""
            _uiState.update {
                it.copy(
                    isLoading = false,
                    nombre = profile?.name ?: "",
                    email = email,
                    moneda = if (profile?.currency.isNullOrEmpty()) "CRC (₡)" else "${profile!!.currency} (₡)",
                    ingresoMensual = profile?.income ?: 0.0
                )
            }
        }
    }
}
