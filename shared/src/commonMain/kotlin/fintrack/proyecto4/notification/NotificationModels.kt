package fintrack.proyecto4.notification

/** Registro de una alerta de presupuesto ya disparada, persistido para poder consultarla
 *  después (aunque hoy no exista todavía una pantalla de historial que la liste). */
data class BudgetAlertNotification(
    val id: String = "",
    val type: String = "BUDGET_ALERT",
    val budgetId: String,
    val categoryName: String,
    val title: String,
    val body: String,
    val usagePct: Float,
    val createdAt: Long,
    val read: Boolean = false
)
