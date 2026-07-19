package fintrack.proyecto4.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object AndroidNotifierContext {
    var appContext: Context? = null
}

private const val CHANNEL_ID = "budget_alerts"
private const val CHANNEL_NAME = "Alertas de presupuesto"

actual fun showLocalNotification(title: String, body: String) {
    val ctx = AndroidNotifierContext.appContext ?: return
    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    try {
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    } catch (_: SecurityException) {
    }
}
