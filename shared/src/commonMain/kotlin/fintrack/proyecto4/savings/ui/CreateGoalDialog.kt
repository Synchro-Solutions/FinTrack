package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        targetAmount: String,
        deadline: String?,
        iconName: String
    ) -> Unit
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var iconName by remember { mutableStateOf("⭐") }
    var showDatePicker by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = FinTrackColors.VioletDark,
        unfocusedLabelColor = colors.textSecondary,
        focusedPlaceholderColor = colors.textSecondary,
        unfocusedPlaceholderColor = colors.textSecondary,
        cursorColor = colors.textPrimary,
        focusedBorderColor = FinTrackColors.VioletDark,
        unfocusedBorderColor = colors.border
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Nueva meta", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { newValue ->
                        targetAmount = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Monto objetivo") },
                    placeholder = { Text("Ej: 400000") },
                    singleLine = true,
                    colors = fieldColors
                )

                Box {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = {},
                        label = { Text("Fecha límite opcional") },
                        placeholder = { Text("Seleccionar fecha") },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha",
                                tint = colors.textSecondary
                            )
                        },
                        colors = fieldColors
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showDatePicker = true }
                            )
                    )
                }

                OutlinedTextField(
                    value = iconName,
                    onValueChange = { iconName = it.take(2) },
                    label = { Text("Ícono") },
                    singleLine = true,
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        targetAmount,
                        deadline.ifBlank { null },
                        iconName.ifBlank { "⭐" }
                    )
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        }
    )

    if (showDatePicker) {
        SavingsDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                deadline = selectedDate
                showDatePicker = false
            }
        )
    }
}