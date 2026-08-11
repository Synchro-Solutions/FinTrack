package fintrack.proyecto4.savings.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.util.formatColones

/**
 * Dialogo propio (no AlertDialog) para que el confeti pueda dibujarse en la misma
 * ventana que la tarjeta: AlertDialog abre su propia Window de Android por encima
 * del contenido de la pantalla, asi que un GoalConfetti compuesto afuera del dialogo
 * queda tapado detras de esa ventana. Aqui el confeti vive dentro del Dialog, detras
 * de la tarjeta en el mismo Box.
 */
@Composable
fun GoalCompletedDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GoalConfetti(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface)
                    .padding(24.dp)
            ) {
                Text(
                    text = "🎉 ¡Meta alcanzada!",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Completaste la meta: ${goal.name}",
                        color = colors.textPrimary
                    )

                    Text(
                        text = "Monto final: ${formatMoney(goal.targetAmount)}",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Excelente trabajo. Sigue creando hábitos financieros positivos.",
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Aceptar", color = colors.primary)
                    }
                }
            }
        }
    }
}

private fun formatMoney(amount: Double): String = formatColones(amount)
