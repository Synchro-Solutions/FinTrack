package fintrack.proyecto4.savings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun GoalDetailDialog(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    onDismiss: () -> Unit,
    onCancelGoal: (SavingsGoal) -> Unit,
    onEditGoal: (SavingsGoal) -> Unit
) {
    val colors = LocalAppColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "GoalDetailProgress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("${goal.iconName} ${goal.name}", color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusBadge(goal.status)

                Text(
                    text = "${goal.progressPercentage}% completado",
                    color = FinTrackColors.GreenPrimary,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = FinTrackColors.GreenPrimary,
                    trackColor = colors.surfaceSecondary
                )

                Text("Ahorrado: ${formatMoney(goal.currentAmount)}", color = colors.textPrimary)
                Text("Objetivo: ${formatMoney(goal.targetAmount)}", color = colors.textPrimary)
                Text("Faltan: ${formatMoney(goal.remainingAmount)}", color = colors.textSecondary)
                Text("Tiempo: ${goal.deadlineLabel}", color = colors.textSecondary)
                Text("Fecha límite: ${goal.deadline ?: "Sin fecha definida"}", color = colors.textSecondary)

                HorizontalDivider(color = colors.divider)

                Text(
                    text = "Historial de abonos",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (contributions.isEmpty()) {
                    Text(
                        text = "Aún no hay abonos registrados.",
                        color = colors.textSecondary
                    )
                } else {
                    contributions.take(5).forEach { contribution ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(contribution.createdAt, color = colors.textSecondary)
                            Text(
                                text = "+${formatMoney(contribution.amount)}",
                                color = FinTrackColors.GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (goal.status == GoalStatus.ACTIVE) {
                    TextButton(onClick = { onEditGoal(goal) }) {
                        Text("Editar", color = FinTrackColors.VioletDark)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = FinTrackColors.GreenPrimary)
                }
            }
        },
        dismissButton = {
            if (goal.status == GoalStatus.ACTIVE) {
                Button(
                    onClick = { onCancelGoal(goal) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackColors.ErrorColor,
                        contentColor = FinTrackColors.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancelar meta")
                }
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun StatusBadge(status: GoalStatus) {
    val text = when (status) {
        GoalStatus.ACTIVE -> "Activa"
        GoalStatus.COMPLETED -> "Completada"
        GoalStatus.CANCELLED -> "Cancelada"
    }

    val color = when (status) {
        GoalStatus.ACTIVE -> FinTrackColors.GreenPrimary
        GoalStatus.COMPLETED -> FinTrackColors.BlueMeta
        GoalStatus.CANCELLED -> FinTrackColors.ErrorColor
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatMoney(amount: Double): String {
    val cleanAmount = amount.toInt()
    val formatted = cleanAmount.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return "₡$formatted"
}