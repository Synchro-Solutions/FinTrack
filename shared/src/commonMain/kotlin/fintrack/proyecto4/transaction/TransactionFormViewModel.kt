package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import fintrack.proyecto4.ocr.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionFormViewModel(
    initialType: TransactionType = TransactionType.EXPENSE
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionFormState(type = initialType)
    )

    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    fun changeType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            type = type,
            selectedCategory = null,
            description = ""
        )
    }

    fun updateAmount(amount: String) {
        val cleanAmount = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amount = cleanAmount)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(paymentMethod = paymentMethod)
    }

    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun reset(initialType: TransactionType) {
        _uiState.value = TransactionFormState(
            type = initialType
        )
    }

    /**
     * Precarga el formulario con datos detectados por el asistente OCR.
     * El comercio detectado se mapea al campo de descripción (no existe un
     * campo "comercio" separado en el modelo de transacción).
     *
     * Solo se escriben los campos que el OCR detectó con confianza. Si algo
     * no se detectó (null), el campo queda **vacío** ("") en vez de conservar
     * cualquier valor por defecto previo del ViewModel — en particular, la
     * fecha NUNCA cae a "hoy" cuando el OCR no la detectó, ya que eso se veía
     * como si fuera un dato real leído del comprobante cuando en realidad fue
     * inventado. El campo vacío se muestra como "Dato no detectado" en la
     * pantalla de confirmación (OcrConfirmScreen) y el usuario debe llenarlo
     * o corregirlo manualmente antes de poder guardar.
     */
    fun prefillFromOcr(result: OcrResult) {
        _uiState.value = _uiState.value.copy(
            amount = result.amount ?: "",
            description = result.merchantName ?: "",
            date = result.date ?: ""
        )
    }

    fun saveTransaction() {
        // TODO: conectar con repositorio cuando exista persistencia real
    }
}