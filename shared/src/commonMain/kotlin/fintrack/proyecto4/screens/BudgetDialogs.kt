package fintrack.proyecto4.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.budget.BudgetItem
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.util.formatColones
import kotlin.math.roundToInt

@Composable
fun BudgetActionsDialog(
    budget: BudgetItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("${budget.categoryIcon} ${budget.categoryName}", color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Límite: ${formatColones(budget.limit.toLong())}",
                    color = colors.textPrimary
                )
                Text(
                    text = "Gastado: ${formatColones(budget.spent.toLong())} (${budget.usagePercentInt}%)",
                    color = colors.textSecondary
                )
                Text(
                    text = "Alerta al ${(budget.alertThreshold * 100).roundToInt()}% usado",
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Editar", color = FinTrackColors.GreenPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDeactivate) {
                Text("Desactivar", color = FinTrackColors.ErrorColor)
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
fun EditBudgetDialog(
    budget: BudgetItem,
    onDismiss: () -> Unit,
    onSave: (newLimit: Double, newThreshold: Float) -> Unit
) {
    val colors = LocalAppColors.current
    var limitText by remember { mutableStateOf(budget.limit.toLong().toString()) }
    var threshold by remember { mutableStateOf(budget.alertThreshold) }

    val parsed = limitText.toDoubleOrNull() ?: 0.0
    val isValid = parsed > 0.0
    val belowSpent = isValid && parsed < budget.spent

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = FinTrackColors.GreenPrimary,
        unfocusedLabelColor = colors.textSecondary,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedBorderColor = FinTrackColors.GreenPrimary,
        unfocusedBorderColor = colors.border
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Editar presupuesto", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { new -> limitText = new.filter { it.isDigit() } },
                    label = { Text("Monto límite (₡)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid && limitText.isNotBlank(),
                    colors = fieldColors
                )

                if (belowSpent) {
                    Text(
                        text = "El límite es menor al gasto actual",
                        color = FinTrackColors.WarningColor,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "Alerta al ${(threshold * 100).roundToInt()}% usado",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 0.5f..0.95f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = FinTrackColors.GreenPrimary,
                        activeTrackColor = FinTrackColors.GreenPrimary,
                        inactiveTrackColor = colors.surfaceSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(parsed, threshold) },
                enabled = isValid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.4f)
                )
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
fun DeactivateBudgetDialog(
    budget: BudgetItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("¿Desactivar presupuesto?", color = colors.textPrimary) },
        text = {
            Text(
                text = "No podrá reactivarlo. Los gastos ya registrados en \"${budget.categoryName}\" mantienen su categoría.",
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.ErrorColor)
            ) {
                Text("Desactivar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}
