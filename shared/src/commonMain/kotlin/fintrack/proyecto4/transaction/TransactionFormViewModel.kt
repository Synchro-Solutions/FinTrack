package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.ocr.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * @param uid Usuario actualmente autenticado (ver AuthClient.currentUserId(), resuelto por la
 *   pantalla que crea este ViewModel). Toda transacción se guarda asociada a este uid.
 * @param editingTransaction Si no es null, el formulario edita esta transacción existente en
 *   vez de crear una nueva (US-14); ya llega completa desde la navegación (ver
 *   Screen.TransactionForm), sin necesidad de volver a pedirla a Firestore.
 */
class TransactionFormViewModel(
    private val repository: TransactionRepository,
    private val uid: String,
    initialType: TransactionType = TransactionType.EXPENSE,
    private val editingTransaction: Transaction? = null
) : ViewModel() {

    val isEditing: Boolean get() = editingTransaction != null

    private val _uiState = MutableStateFlow(
        editingTransaction?.toFormState() ?: TransactionFormState(type = initialType)
    )
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun changeType(type: TransactionType) {
        _uiState.update { it.copy(type = type, selectedCategory = null, description = "") }
    }

    fun updateAmount(amount: String) {
        val cleanAmount = amount.filter { it.isDigit() }
        _uiState.update { it.copy(amount = cleanAmount) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = paymentMethod) }
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(date = date) }
    }

    fun reset(initialType: TransactionType) {
        _uiState.value = TransactionFormState(type = initialType)
    }

    /**
     * Precarga el formulario con datos detectados por el asistente OCR.
     * El comercio detectado se mapea al campo de descripción (no existe, ni debe crearse,
     * un campo "comercio" separado en el modelo de transacción).
     *
     * Solo se escriben los campos que el OCR detectó con confianza. Si algo no se detectó
     * (null), el campo queda **vacío** ("") en vez de conservar cualquier valor por defecto
     * previo del ViewModel — en particular, la fecha NUNCA cae a "hoy" cuando el OCR no la
     * detectó, ya que eso se vería como si fuera un dato real leído del comprobante cuando en
     * realidad fue inventado. El campo vacío se muestra como "Dato no detectado" en la
     * pantalla de confirmación (OcrConfirmScreen) y el usuario debe llenarlo o corregirlo
     * manualmente antes de poder guardar.
     */
    fun prefillFromOcr(result: OcrResult) {
        _uiState.update {
            it.copy(
                amount = result.amount ?: "",
                description = result.merchantName ?: "",
                date = result.date ?: ""
            )
        }
    }

    fun saveTransaction(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        if (!state.isValid) return

        val amount = state.amount.toLongOrNull()
        if (amount == null) {
            _saveError.value = "Ingrese un monto válido"
            return
        }

        if (uid.isBlank()) {
            _saveError.value = "Debes iniciar sesión para guardar movimientos"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val transaction = Transaction(
                    id = editingTransaction?.id ?: "",
                    type = state.type,
                    amount = amount,
                    description = state.description.trim(),
                    category = state.selectedCategory.orEmpty(),
                    paymentMethod = state.paymentMethod,
                    date = state.date,
                    createdAt = editingTransaction?.createdAt ?: currentEpochMillis()
                )

                if (isEditing) {
                    repository.updateTransaction(uid, transaction)
                } else {
                    repository.addTransaction(uid, transaction)
                }

                _saveError.value = null
                onSaved()
            } catch (e: Exception) {
                _saveError.value = "No se pudo guardar el movimiento. Intenta de nuevo."
            } finally {
                _isSaving.value = false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun Transaction.toFormState() = TransactionFormState(
        type = type,
        amount = amount.toString(),
        description = description,
        selectedCategory = category,
        paymentMethod = paymentMethod,
        date = date
    )
}
