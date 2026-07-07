package fintrack.proyecto4.savings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal

@Composable
fun GoalDetailDialog(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    onDismiss: () -> Unit,
    onCancelGoal: (SavingsGoal) -> Unit,
    onEditGoal: (SavingsGoal) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "GoalDetailProgress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111C2E),
        title = {
            Text("${goal.iconName} ${goal.name}", color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusBadge(goal.status)

                Text(
                    text = "${goal.progressPercentage}% completado",
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF22C55E),
                    trackColor = Color(0xFF263447)
                )

                Text("Ahorrado: ${formatMoney(goal.currentAmount)}", color = Color.White)
                Text("Objetivo: ${formatMoney(goal.targetAmount)}", color = Color.White)
                Text("Faltan: ${formatMoney(goal.remainingAmount)}", color = Color(0xFF94A3B8))
                Text("Tiempo: ${goal.deadlineLabel}", color = Color(0xFF94A3B8))
                Text("Fecha límite: ${goal.deadline ?: "Sin fecha definida"}", color = Color(0xFF94A3B8))

                HorizontalDivider(color = Color(0xFF263447))

                Text(
                    text = "Historial de abonos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (contributions.isEmpty()) {
                    Text(
                        text = "Aún no hay abonos registrados.",
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    contributions.take(5).forEach { contribution ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(contribution.createdAt, color = Color(0xFF94A3B8))
                            Text(
                                text = "+${formatMoney(contribution.amount)}",
                                color = Color(0xFF22C55E),
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
                        Text("Editar", color = Color(0xFF8B5CF6))
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color(0xFF22C55E))
                }
            }
        },
        dismissButton = {
            if (goal.status == GoalStatus.ACTIVE) {
                Button(
                    onClick = { onCancelGoal(goal) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
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
        GoalStatus.ACTIVE -> Color(0xFF22C55E)
        GoalStatus.COMPLETED -> Color(0xFF3B82F6)
        GoalStatus.CANCELLED -> Color(0xFFEF4444)
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