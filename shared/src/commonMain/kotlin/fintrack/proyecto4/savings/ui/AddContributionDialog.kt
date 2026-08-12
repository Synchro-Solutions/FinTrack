package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun AddContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSave: (amount: String) -> Unit
) {
    val colors = LocalAppColors.current
    var amount by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = Color(0xFF8B5CF6),
        unfocusedLabelColor = colors.textSecondary,
        focusedPlaceholderColor = colors.textSecondary,
        unfocusedPlaceholderColor = colors.textSecondary,
        cursorColor = colors.textPrimary,
        focusedBorderColor = Color(0xFF8B5CF6),
        unfocusedBorderColor = colors.textSecondary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Abonar a ${goal.name}",
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Progreso actual: ${goal.progressPercentage}%",
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        amount = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Monto del abono") },
                    placeholder = { Text("Ej: 50000") },
                    singleLine = true,
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(amount) },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancelar",
                    color = colors.textSecondary
                )
            }
        }
    )
}