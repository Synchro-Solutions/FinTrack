package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.ai.AnomalyAlertBus
import fintrack.proyecto4.ai.AnomalyDetector
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
    private val editingTransaction: Transaction? = null,
    private val anomalyDetector: AnomalyDetector = AnomalyDetector(),
    private val categoryRepository: CustomCategoryRepository = NoOpCustomCategoryRepository()
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

    private val _customCategories = MutableStateFlow<List<CustomCategory>>(emptyList())
    val customCategories: StateFlow<List<CustomCategory>> = _customCategories.asStateFlow()

    private val _categoryFormError = MutableStateFlow<String?>(null)
    val categoryFormError: StateFlow<String?> = _categoryFormError.asStateFlow()

    init {
        viewModelScope.launch {
            _customCategories.value = try {
                categoryRepository.getCategories(uid)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun changeType(type: TransactionType) {
        _uiState.update { it.copy(type = type, selectedCategory = null, description = "") }
    }

    fun updateAmount(amount: String) {
        val cleanAmount = amount.filter { it.isDigit() }
        _uiState.update { it.copy(amount = cleanAmount) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description.take(MaxDescriptionLength)) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearCategoryFormError() {
        _categoryFormError.value = null
    }

    /** Nombre ya usado por una categoría fija o personalizada del mismo tipo, excluyendo
     *  [excludingId] (para permitir guardar una edición sin nombre, sin chocar consigo misma). */
    private fun isDuplicateCategoryName(name: String, type: TransactionType, excludingId: String?): Boolean {
        val fixed = if (type == TransactionType.EXPENSE) ExpenseCategories else IncomeCategories
        val trimmed = name.trim()
        val fixedClash = fixed.any { it.equals(trimmed, ignoreCase = true) }
        val customClash = _customCategories.value.any {
            it.id != excludingId && it.type == type && it.name.equals(trimmed, ignoreCase = true)
        }
        return fixedClash || customClash
    }

    fun createCategory(name: String, icon: String?, onSuccess: () -> Unit = {}) {
        val trimmed = name.trim()
        val type = _uiState.value.type
        if (trimmed.isBlank()) {
            _categoryFormError.value = "Ingresa un nombre para la categoría."
            return
        }
        if (isDuplicateCategoryName(trimmed, type, excludingId = null)) {
            _categoryFormError.value = "Ya tienes una categoría con este nombre"
            return
        }
        viewModelScope.launch {
            try {
                val created = categoryRepository.addCategory(
                    uid,
                    CustomCategory(name = trimmed, type = type, icon = icon?.trim()?.ifBlank { null })
                )
                _customCategories.update { it + created }
                _categoryFormError.value = null
                selectCategory(created.name)
                onSuccess()
            } catch (e: Exception) {
                _categoryFormError.value = "No se pudo guardar la categoría. Intenta de nuevo."
            }
        }
    }

    fun updateCategory(original: CustomCategory, name: String, icon: String?, onSuccess: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _categoryFormError.value = "Ingresa un nombre para la categoría."
            return
        }
        if (isDuplicateCategoryName(trimmed, original.type, excludingId = original.id)) {
            _categoryFormError.value = "Ya tienes una categoría con este nombre"
            return
        }
        viewModelScope.launch {
            try {
                val updated = original.copy(name = trimmed, icon = icon?.trim()?.ifBlank { null })
                categoryRepository.updateCategory(uid, updated)
                _customCategories.update { list -> list.map { if (it.id == updated.id) updated else it } }
                _categoryFormError.value = null
                // Si la categoría editada estaba seleccionada, refleja el nombre nuevo.
                if (_uiState.value.selectedCategory == original.name) {
                    selectCategory(updated.name)
                }
                onSuccess()
            } catch (e: Exception) {
                _categoryFormError.value = "No se pudo guardar la categoría. Intenta de nuevo."
            }
        }
    }

    fun deleteCategory(category: CustomCategory) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(uid, category.id)
                _customCategories.update { list -> list.filter { it.id != category.id } }
                reassignTransactionsToFallback(category.name)
                if (_uiState.value.selectedCategory == category.name) {
                    _uiState.update { it.copy(selectedCategory = null) }
                }
            } catch (e: Exception) {
                _categoryFormError.value = "No se pudo eliminar la categoría. Intenta de nuevo."
            }
        }
    }

    /** Al borrar una categoría personalizada, las transacciones que la usaban quedarían con
     *  un nombre de categoría "huérfano" (ya no aparece en ningún selector). Se reasignan a
     *  "Otro" — existe como categoría fija tanto para gastos como para ingresos — en vez de
     *  dejarlas apuntando a algo que ya no existe. Si esto falla, no bloquea el borrado de la
     *  categoría en sí; el usuario puede reasignar esas transacciones a mano después. */
    private suspend fun reassignTransactionsToFallback(deletedCategoryName: String) {
        try {
            val affected = repository.getTransactions(uid).filter { it.category == deletedCategoryName }
            affected.forEach { transaction ->
                repository.updateTransaction(uid, transaction.copy(category = "Otro"))
            }
        } catch (e: Exception) {
            // Silencioso a propósito, ver comentario de la función.
        }
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
                    val priorHistory = if (transaction.type == TransactionType.EXPENSE) {
                        try { repository.getTransactions(uid) } catch (_: Exception) { emptyList() }
                    } else {
                        emptyList()
                    }

                    repository.addTransaction(uid, transaction)

                    if (transaction.type == TransactionType.EXPENSE) {
                        try {
                            val alert = anomalyDetector.analyze(transaction, priorHistory)
                            if (alert != null) AnomalyAlertBus.post(alert)
                        } catch (_: Exception) {
                        }
                    }
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
