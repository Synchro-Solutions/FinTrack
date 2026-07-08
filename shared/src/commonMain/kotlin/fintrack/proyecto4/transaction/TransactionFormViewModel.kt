package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.ocr.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @param editingTransactionId Si no es null, el formulario carga y edita la transacción
 *   existente con ese id (US-14) en vez de crear una nueva. Se busca directamente en el
 *   caché en memoria de [TransactionRepository] (poblado previamente por la pantalla de
 *   Movimientos), sin ida y vuelta a Firestore.
 */
class TransactionFormViewModel(
    initialType: TransactionType = TransactionType.EXPENSE,
    private val editingTransactionId: String? = null,
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    val isEditing: Boolean get() = editingTransactionId != null

    private val _uiState = MutableStateFlow(
        editingTransactionId
            ?.let { id -> repository.getTransaction(id) }
            ?.toFormState()
            ?: TransactionFormState(type = initialType)
    )

    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

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

    fun saveTransaction(onResult: (Result<Transaction>) -> Unit = {}) {
        val state = _uiState.value
        if (!state.isValid) return

        val amount = state.amount.toLongOrNull()
        if (amount == null) {
            _saveError.value = "Ingrese un monto válido"
            return
        }

        viewModelScope.launch {
            val result = if (editingTransactionId != null) {
                repository.updateTransaction(
                    id = editingTransactionId,
                    type = state.type,
                    amount = amount,
                    description = state.description,
                    category = state.selectedCategory.orEmpty(),
                    paymentMethod = state.paymentMethod,
                    date = state.date
                )
            } else {
                repository.createTransaction(
                    type = state.type,
                    amount = amount,
                    description = state.description,
                    category = state.selectedCategory.orEmpty(),
                    paymentMethod = state.paymentMethod,
                    date = state.date
                )
            }

            _saveError.value = result.exceptionOrNull()?.message
            onResult(result)
        }
    }

    private fun Transaction.toFormState() = TransactionFormState(
        type = type,
        amount = amount.toString(),
        description = description,
        selectedCategory = category,
        paymentMethod = paymentMethod,
        date = date
    )
}
