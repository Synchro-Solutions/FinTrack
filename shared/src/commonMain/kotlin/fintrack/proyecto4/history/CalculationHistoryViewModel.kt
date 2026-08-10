package fintrack.proyecto4.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [tipoSeleccionado] null significa "Todos". */
data class CalculationHistoryUiState(
    val isLoading: Boolean = true,
    val calculations: List<SavedCalculation> = emptyList(),
    val tipoSeleccionado: CalculationType? = null
) {
    val calculacionesFiltradas: List<SavedCalculation>
        get() = if (tipoSeleccionado == null) {
            calculations
        } else {
            calculations.filter { it.tipo == tipoSeleccionado }
        }
}

class CalculationHistoryViewModel(
    private val repository: CalculationHistoryRepository = NoOpCalculationHistoryRepository(),
    private val uid: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculationHistoryUiState())
    val uiState: StateFlow<CalculationHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val calculations = try {
                repository.getCalculations(uid)
            } catch (_: Exception) {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    calculations = calculations.sortedByDescending { calculo -> calculo.fechaCalculo }
                )
            }
        }
    }

    fun onTipoSeleccionado(tipo: CalculationType?) {
        _uiState.update { it.copy(tipoSeleccionado = tipo) }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteCalculation(uid, id)
            } catch (_: Exception) {
                // Si falla el borrado remoto, load() en el siguiente refresh reflejara el estado real.
            }
            _uiState.update { state ->
                state.copy(calculations = state.calculations.filterNot { it.id == id })
            }
        }
    }
}
