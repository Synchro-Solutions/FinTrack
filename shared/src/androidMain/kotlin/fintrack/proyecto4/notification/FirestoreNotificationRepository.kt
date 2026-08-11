package fintrack.proyecto4.notification

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Persiste alertas de presupuesto en users/{uid}/notifications/{notificationId}.
 * Ya cubierto por las reglas de seguridad existentes del proyecto.
 */
class FirestoreNotificationRepository : NotificationRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("notifications")

    override suspend fun addNotification(uid: String, notification: BudgetAlertNotification) {
        col(uid).document.set(
            mapOf(
                "type" to notification.type,
                "budgetId" to notification.budgetId,
                "categoryName" to notification.categoryName,
                "title" to notification.title,
                "body" to notification.body,
                "usagePct" to notification.usagePct.toDouble(),
                "createdAt" to notification.createdAt,
                "read" to notification.read
            )
        )
    }

    override suspend fun getNotifications(uid: String): List<BudgetAlertNotification> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    BudgetAlertNotification(
                        id = doc.id,
                        type = doc.get<String?>("type") ?: "BUDGET_ALERT",
                        budgetId = doc.get("budgetId"),
                        categoryName = doc.get("categoryName"),
                        title = doc.get("title"),
                        body = doc.get("body"),
                        usagePct = doc.get<Double>("usagePct").toFloat(),
                        createdAt = doc.get<Long?>("createdAt") ?: 0L,
                        read = doc.get<Boolean?>("read") ?: false
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
