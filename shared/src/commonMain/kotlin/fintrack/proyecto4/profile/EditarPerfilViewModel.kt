package fintrack.proyecto4.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.onboarding.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditarPerfilState(
    val isLoading: Boolean = true,
    val name: String = "",
    val income: String = "",
    val currency: String = "CRC",
    val photoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val error: String? = null
)

class EditarPerfilViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val uid: String,
    private val uploadPhoto: suspend (String) -> Result<String>
) : ViewModel() {

    private val _state = MutableStateFlow(EditarPerfilState())
    val state: StateFlow<EditarPerfilState> = _state

    // Se preservan tal cual venían: esta pantalla no vuelve a pedir el consentimiento de onboarding.
    private var privacyAccepted = true
    private var termsAccepted = true

    init {
        viewModelScope.launch {
            val profile = runCatching { onboardingRepository.getProfile(uid) }.getOrNull()
            privacyAccepted = profile?.privacyAccepted ?: true
            termsAccepted = profile?.termsAccepted ?: true
            _state.value = _state.value.copy(
                isLoading = false,
                name = profile?.name ?: "",
                income = (profile?.income ?: 0.0).takeIf { it > 0 }?.toString() ?: "",
                currency = profile?.currency?.takeIf { it.isNotBlank() } ?: "CRC",
                photoUrl = profile?.photoPath
            )
        }
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value, error = null)
    }

    fun setIncome(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _state.value = _state.value.copy(income = value)
        }
    }

    fun setCurrency(value: String) {
        _state.value = _state.value.copy(currency = value)
    }

    fun onPhotoPicked(localPath: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPhoto = true, error = null)
            uploadPhoto(localPath).fold(
                onSuccess = { url ->
                    _state.value = _state.value.copy(isUploadingPhoto = false, photoUrl = url)
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        error = "No se pudo subir la foto. Intenta de nuevo."
                    )
                }
            )
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "El nombre no puede estar vacío")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                onboardingRepository.saveProfile(
                    uid = uid,
                    profile = UserProfile(
                        name = s.name.trim(),
                        photoPath = s.photoUrl,
                        income = s.income.toDoubleOrNull() ?: 0.0,
                        currency = s.currency,
                        privacyAccepted = privacyAccepted,
                        termsAccepted = termsAccepted
                    )
                )
                _state.value = _state.value.copy(isSaving = false, savedOk = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "No se pudo guardar el perfil. Intenta de nuevo."
                )
            }
        }
    }
}
