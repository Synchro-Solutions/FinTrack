package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.notifications.AppNotification
import fintrack.proyecto4.notifications.NotificationRepository
import fintrack.proyecto4.notifications.NotificationType
import fintrack.proyecto4.notifications.NoOpNotificationRepository
import fintrack.proyecto4.notifications.NotificationsViewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun NotificationsScreen(
    notificationRepository: NotificationRepository = NoOpNotificationRepository(),
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) { NotificationsViewModel(notificationRepository, uid) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Notificaciones",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (!state.isLoading && state.notifications.isEmpty()) {
            EmptyNotificationsState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationCard(notification)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification) {
    val colors = LocalAppColors.current
    val (icon, accent) = when (notification.type) {
        NotificationType.BUDGET_EXCEEDED -> "🚨" to FinTrackColors.ErrorColor
        NotificationType.BUDGET_ALERT -> "⚠️" to FinTrackColors.WarningColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = notification.body,
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔔", fontSize = 60.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sin notificaciones",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Aquí verás las alertas de tus presupuestos.",
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
