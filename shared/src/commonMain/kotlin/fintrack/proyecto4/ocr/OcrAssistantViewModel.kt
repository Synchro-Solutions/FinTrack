package fintrack.proyecto4.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.ai.ReceiptAiService
import fintrack.proyecto4.transaction.CustomCategoryRepository
import fintrack.proyecto4.transaction.ExpenseCategories
import fintrack.proyecto4.transaction.NoOpCustomCategoryRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OcrAssistantViewModel(
    private val recognizeText: suspend (imagePath: String) -> String,
    private val categoryRepository: CustomCategoryRepository = NoOpCustomCategoryRepository(),
    private val uid: String = "",
    private val receiptAiService: ReceiptAiService = ReceiptAiService()
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

                val regexResult = TicketParser.parse(rawText)
                val result = enhanceWithAi(rawText, regexResult)
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

    /**
     * Le pide a la IA que corrija/complete lo que detectó [TicketParser] (errores típicos de
     * OCR, categoría sugerida). Si la IA falla (sin conexión, respuesta inválida, etc.), se
     * conserva el resultado del parser por regex tal cual — la IA es una mejora, no un
     * requisito para que el asistente OCR funcione.
     */
    private suspend fun enhanceWithAi(rawText: String, regexResult: OcrResult): OcrResult {
        val categories = try {
            val custom = categoryRepository.getCategories(uid)
                .filter { it.type == TransactionType.EXPENSE }
                .map { it.name }
            (ExpenseCategories + custom).distinct()
        } catch (_: Exception) {
            ExpenseCategories
        }

        val aiResult = receiptAiService.enhance(rawText, categories).getOrNull()
            ?: return regexResult

        val aiAmount = aiResult.monto
            ?.takeIf { it > 0 }
            ?.let { it.toLong().toString() }

        val aiDate = aiResult.fecha
            ?.takeIf { DATE_REGEX.matches(it) }

        val aiMerchant = aiResult.comercio?.trim()?.takeIf { it.isNotBlank() }

        val aiCategory = aiResult.categoria
            ?.trim()
            ?.let { candidate -> categories.firstOrNull { it.equals(candidate, ignoreCase = true) } }

        return regexResult.copy(
            amount = aiAmount ?: regexResult.amount,
            date = aiDate ?: regexResult.date,
            merchantName = aiMerchant ?: regexResult.merchantName,
            suggestedCategory = aiCategory,
            aiEnhanced = true,
            amountFromAi = aiAmount != null,
            dateFromAi = aiDate != null,
            merchantFromAi = aiMerchant != null
        )
    }

    fun reset() {
        _uiState.value = OcrAssistantUiState()
    }

    private companion object {
        val DATE_REGEX = Regex("""\d{2}/\d{2}/\d{4}""")
    }
}
