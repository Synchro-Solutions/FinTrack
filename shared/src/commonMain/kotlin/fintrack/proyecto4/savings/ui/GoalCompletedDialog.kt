package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun GoalCompletedDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "🎉 ¡Meta alcanzada!",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Completaste la meta: ${goal.name}",
                    color = colors.textPrimary
                )

                Text(
                    text = "Monto final: ${formatMoney(goal.targetAmount)}",
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Excelente trabajo. Sigue creando hábitos financieros positivos.",
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar", color = Color(0xFF22C55E))
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
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