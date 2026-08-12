package fintrack.proyecto4.notifications

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Persiste notificaciones en users/{uid}/notifications/{id} — ruta ya habilitada en
 * firestore.rules (match /users/{userId}/notifications/{notificationId}, lectura/escritura
 * del dueño), mismo patrón que FirestoreTransactionRepository/FirestoreBudgetRepository.
 *
 * El ordenamiento (createdAt DESC) y el LIMIT 50 se aplican en Kotlin tras leer la
 * colección, igual que el resto de repositorios del proyecto, para no requerir índices
 * compuestos en Firestore.
 */
@OptIn(ExperimentalTime::class)
class FirestoreNotificationRepository : NotificationRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("notifications")

    private fun retentionCutoffMillis(): Long =
        Clock.System.now().toEpochMilliseconds() - NotificationRetentionDays * 24L * 60 * 60 * 1000

    override suspend fun getNotifications(uid: String): List<AppNotification> {
        return try {
            val cutoff = retentionCutoffMillis()
            val all = col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    val createdAt = doc.get<Long?>("createdAt") ?: 0L
                    doc.id to AppNotification(
                        id = doc.id,
                        title = doc.get<String?>("title") ?: "",
                        body = doc.get<String?>("body") ?: "",
                        type = doc.get<String?>("type")
                            ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
                            ?: NotificationType.GENERAL,
                        isRead = doc.get<Boolean?>("isRead") ?: false,
                        createdAt = createdAt
                    )
                }.getOrNull()
            }

            // Retención: las de más de 30 días se eliminan físicamente y no se devuelven.
            val (expired, vigentes) = all.partition { it.second.createdAt < cutoff }
            expired.forEach { (id, _) -> runCatching { col(uid).document(id).delete() } }

            vigentes
                .map { it.second }
                .sortedByDescending { it.createdAt }
                .take(NotificationPageLimit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun unreadCount(uid: String): Int {
        return try {
            col(uid).get().documents.count { doc ->
                (doc.get<Boolean?>("isRead") ?: false) == false
            }
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun markAllAsRead(uid: String) {
        try {
            col(uid).get().documents
                .filter { (it.get<Boolean?>("isRead") ?: false) == false }
                .forEach { doc -> runCatching { col(uid).document(doc.id).update("isRead" to true) } }
        } catch (e: Exception) {
            // Best-effort: si falla, el badge simplemente se recalcula en la próxima carga.
        }
    }

    override suspend fun deleteNotification(uid: String, notificationId: String) {
        col(uid).document(notificationId).delete()
    }

    override suspend fun addNotification(uid: String, notification: AppNotification): AppNotification {
        val ref = if (notification.id.isBlank()) col(uid).document else col(uid).document(notification.id)
        ref.set(toMap(notification))
        return notification.copy(id = ref.id)
    }

    private fun toMap(notification: AppNotification) = mapOf(
        "title" to notification.title,
        "body" to notification.body,
        "type" to notification.type.name,
        "isRead" to notification.isRead,
        "createdAt" to notification.createdAt
    )
}
