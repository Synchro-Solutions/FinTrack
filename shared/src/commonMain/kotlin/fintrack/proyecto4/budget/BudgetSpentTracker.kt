package fintrack.proyecto4.budget

import fintrack.proyecto4.notification.BudgetAlertNotification
import fintrack.proyecto4.notification.NoOpNotificationRepository
import fintrack.proyecto4.notification.NotificationRepository
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.transaction.parseFormFieldDate
import kotlinx.datetime.number
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Recalcula `spent` de un presupuesto sumando desde cero las transacciones EXPENSE de la
 * categoría dentro del período actual (en vez de incrementar/decrementar un contador, que se
 * desincroniza fácilmente con ediciones/eliminaciones), y dispara la alerta de presupuesto
 * (persistencia + notificación local) cuando corresponde. Único punto de entrada usado tanto al
 * guardar/editar como al eliminar una transacción.
 */
class BudgetSpentTracker(
    private val budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    private val transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    private val notificationRepository: NotificationRepository = NoOpNotificationRepository(),
    private val showLocalNotification: (title: String, body: String) -> Unit = { _, _ -> }
) {
    suspend fun recalculateAndMaybeAlert(uid: String, categoryName: String) {
        if (categoryName.isBlank() || uid.isBlank()) return

        val budget = try {
            budgetRepository.getBudgets(uid).firstOrNull { it.categoryName == categoryName }
        } catch (_: Exception) {
            null
        } ?: return

        val period = currentBudgetPeriodKey()
        val transactions = try {
            transactionRepository.getTransactions(uid)
        } catch (_: Exception) {
            emptyList()
        }

        val newSpent = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.category == categoryName &&
                    isInPeriod(it.date, period)
            }
            .sumOf { it.amount }
            .toDouble()

        val previousAlertSent = if (budget.spentPeriodKey == period) budget.alertSent else false
        val newUsagePct = if (budget.limit > 0) (newSpent / budget.limit).toFloat() else 0f
        val shouldAlert = !previousAlertSent && newUsagePct >= budget.alertThreshold

        try {
            budgetRepository.updateSpentTracking(
                uid = uid,
                budgetId = budget.id,
                spent = newSpent,
                spentPeriodKey = period,
                alertSent = previousAlertSent || shouldAlert
            )
        } catch (_: Exception) {
            return
        }

        if (shouldAlert) sendAlert(uid, budget, newUsagePct)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun sendAlert(uid: String, budget: BudgetItem, usagePct: Float) {
        val pct = (usagePct * 100).toInt()
        val title = "Alerta de presupuesto"
        val body = "Has gastado el $pct% de tu presupuesto de ${budget.categoryName}."

        try {
            notificationRepository.addNotification(
                uid,
                BudgetAlertNotification(
                    budgetId = budget.id,
                    categoryName = budget.categoryName,
                    title = title,
                    body = body,
                    usagePct = usagePct,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        } catch (_: Exception) {
            // La notificación de sistema igual se muestra aunque falle la persistencia.
        }

        showLocalNotification(title, body)
    }

    private fun isInPeriod(dateStr: String, periodKey: String): Boolean {
        val date = parseFormFieldDate(dateStr) ?: return false
        val month = date.month.number.toString().padStart(2, '0')
        return "${date.year}-$month" == periodKey
    }
}
