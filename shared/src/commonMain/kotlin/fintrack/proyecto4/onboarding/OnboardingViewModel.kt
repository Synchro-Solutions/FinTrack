package fintrack.proyecto4.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OnboardingState(
    val step: Int = 1,
    val prevStep: Int = 1,
    val name: String = "",
    val photoPath: String? = null,
    val income: String = "",
    val currency: String = "CRC",
    val privacyAccepted: Boolean = false,
    val termsAccepted: Boolean = false,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val error: String? = null
)

data class CurrencyOption(val code: String, val label: String, val symbol: String)

val CURRENCIES = listOf(
    CurrencyOption("CRC", "Colón costarricense", "₡"),
    CurrencyOption("USD", "Dólar estadounidense", "$"),
    CurrencyOption("EUR", "Euro", "€"),
    CurrencyOption("MXN", "Peso mexicano", "$"),
    CurrencyOption("COP", "Peso colombiano", "$"),
    CurrencyOption("BRL", "Real brasileño", "R$")
)

class OnboardingViewModel(
    private val repository: OnboardingRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun setName(name: String) { _state.value = _state.value.copy(name = name) }
    fun setPhoto(path: String?) { _state.value = _state.value.copy(photoPath = path) }
    fun setIncome(income: String) { _state.value = _state.value.copy(income = income) }
    fun setCurrency(currency: String) { _state.value = _state.value.copy(currency = currency) }
    fun setPrivacy(accepted: Boolean) { _state.value = _state.value.copy(privacyAccepted = accepted) }
    fun setTerms(accepted: Boolean) { _state.value = _state.value.copy(termsAccepted = accepted) }

    fun canProceedStep1(): Boolean = _state.value.name.isNotBlank()
    fun canFinish(): Boolean = _state.value.privacyAccepted && _state.value.termsAccepted

    fun goNext() {
        val s = _state.value
        if (s.step < 3) _state.value = s.copy(prevStep = s.step, step = s.step + 1)
    }

    fun goBack() {
        val s = _state.value
        if (s.step > 1) _state.value = s.copy(prevStep = s.step, step = s.step - 1)
    }

    fun saveAndFinish(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            try {
                repository.saveProfile(
                    uid = uid,
                    profile = UserProfile(
                        name = s.name,
                        photoPath = s.photoPath,
                        income = s.income.toDoubleOrNull() ?: 0.0,
                        currency = s.currency,
                        privacyAccepted = s.privacyAccepted,
                        termsAccepted = s.termsAccepted
                    )
                )
                _state.value = _state.value.copy(isSaving = false, savedOk = true)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al guardar el perfil. Intenta de nuevo."
                )
            }
        }
    }
}
