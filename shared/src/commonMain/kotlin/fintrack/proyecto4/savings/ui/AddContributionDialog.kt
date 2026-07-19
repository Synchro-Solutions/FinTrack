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

@Composable
fun AddContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSave: (amount: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color(0xFF8B5CF6),
        unfocusedLabelColor = Color(0xFF94A3B8),
        focusedPlaceholderColor = Color(0xFF64748B),
        unfocusedPlaceholderColor = Color(0xFF64748B),
        cursorColor = Color.White,
        focusedBorderColor = Color(0xFF8B5CF6),
        unfocusedBorderColor = Color(0xFF64748B)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111C2E),
        title = {
            Text(
                text = "Abonar a ${goal.name}",
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Progreso actual: ${goal.progressPercentage}%",
                    color = Color(0xFF94A3B8)
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
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}