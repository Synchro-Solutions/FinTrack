package fintrack.proyecto4.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OcrAssistantViewModel(
    private val recognizeText: suspend (imagePath: String) -> String
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrAssistantUiState())
    val uiState: StateFlow<OcrAssistantUiState> = _uiState.asStateFlow()

    fun processImage(imagePath: String) {
        _uiState.value = OcrAssistantUiState(status = OcrAssistantStatus.Processing, imagePath = imagePath)

        viewModelScope.launch {
            try {
                val rawText = recognizeText(imagePath)

                if (rawText.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        status = OcrAssistantStatus.Error,
                        errorMessage = "No se detectó texto en la imagen. Intenta con otra foto."
                    )
                    return@launch
                }

                val result = TicketParser.parse(rawText)
                _uiState.value = _uiState.value.copy(status = OcrAssistantStatus.Success, result = result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = OcrAssistantStatus.Error,
                    errorMessage = e.message ?: "No se pudo procesar la imagen"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = OcrAssistantUiState()
    }
}
