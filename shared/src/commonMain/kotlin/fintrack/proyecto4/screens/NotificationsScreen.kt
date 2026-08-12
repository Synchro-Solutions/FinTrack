package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.notifications.AppNotification
import fintrack.proyecto4.notifications.NoOpNotificationRepository
import fintrack.proyecto4.notifications.NotificationRepository
import fintrack.proyecto4.notifications.NotificationType
import fintrack.proyecto4.notifications.NotificationsViewModel
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.glassCard
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Bandeja de notificaciones (US-43). Lista ordenada por fecha (DESC), badge de no leídas
 * en el Dashboard, marcado como leídas al abrir y swipe para eliminar con confirmación.
 */
@Composable
fun NotificationsScreen(
    notificationRepository: NotificationRepository = NoOpNotificationRepository(),
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) { NotificationsViewModel(notificationRepository, uid) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<AppNotification?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Notificaciones", onBack = onBack)

        when {
            state.isEmpty -> EmptyNotifications(modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notifications, key = { it.id }) { notification ->
                    SwipeableNotification(
                        notification = notification,
                        onRequestDelete = { pendingDelete = notification }
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar notificación") },
            text = { Text("¿Seguro que deseas eliminar esta notificación? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    pendingDelete = null
                }) {
                    Text("Eliminar", color = FinTrackColors.ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SwipeableNotification(
    notification: AppNotification,
    onRequestDelete: () -> Unit
) {
    // confirmValueChange devuelve false a propósito: no elimina en el swipe, solo dispara
    // el diálogo de confirmación y deja que la tarjeta regrese a su posición. El borrado
    // real ocurre únicamente si el usuario confirma (ver AlertDialog).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteSwipeBackground() }
    ) {
        NotificationRow(notification)
    }
}

@Composable
private fun DeleteSwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(FinTrackColors.ErrorColor)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Eliminar",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun NotificationRow(notification: AppNotification) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = notification.type.emoji(), fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!notification.isRead) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }
            if (notification.body.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notification.body,
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = notificationTimeLabel(notification.createdAt),
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No tienes notificaciones",
            color = colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun NotificationType.emoji(): String = when (this) {
    NotificationType.BUDGET -> "💸"
    NotificationType.GOAL -> "🎯"
    NotificationType.TIP -> "💡"
    NotificationType.GENERAL -> "🔔"
}

/**
 * Etiqueta relativa ("Hace 5 min", "Ayer", "Hace 3 d") para la fecha de la notificación.
 */
@OptIn(ExperimentalTime::class)
private fun notificationTimeLabel(
    createdAt: Long,
    now: Long = Clock.System.now().toEpochMilliseconds()
): String {
    val diff = now - createdAt
    if (diff < 60_000) return "Ahora"
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days == 1L -> "Ayer"
        else -> "Hace $days d"
    }
}
