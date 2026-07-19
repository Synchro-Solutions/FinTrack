package fintrack.proyecto4.notifications

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

class FirestoreNotificationRepository : NotificationRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("notifications")

    override suspend fun getNotifications(uid: String): List<AppNotification> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    AppNotification(
                        id = doc.id,
                        type = try {
                            NotificationType.valueOf(doc.get<String>("type"))
                        } catch (_: Exception) {
                            NotificationType.BUDGET_ALERT
                        },
                        title = doc.get("title"),
                        body = doc.get("body"),
                        read = try { doc.get<Boolean>("read") } catch (_: Exception) { false },
                        createdAt = try { doc.get<Long>("createdAt") } catch (_: Exception) { 0L }
                    )
                }.getOrNull()
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addNotification(uid: String, notification: AppNotification) {
        col(uid).add(
            mapOf(
                "type" to notification.type.name,
                "title" to notification.title,
                "body" to notification.body,
                "read" to notification.read,
                "createdAt" to notification.createdAt
            )
        )
    }

    override suspend fun markAllRead(uid: String) {
        try {
            col(uid).get().documents.forEach { doc ->
                if (doc.get<Boolean>("read") != true) {
                    col(uid).document(doc.id).update("read" to true)
                }
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun unreadCount(uid: String): Int {
        return try {
            col(uid).get().documents.count {
                try { it.get<Boolean>("read") != true } catch (_: Exception) { true }
            }
        } catch (e: Exception) {
            0
        }
    }
}
