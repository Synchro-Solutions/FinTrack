package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.savings.model.SavingsGoal

@Composable
fun EditGoalDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSave: (name: String, deadline: String?, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var deadline by remember { mutableStateOf(goal.deadline ?: "") }
    var iconName by remember { mutableStateOf(goal.iconName) }
    var showDatePicker by remember { mutableStateOf(false) }

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
        title = { Text("Editar meta", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    colors = fieldColors
                )

                Box {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = {},
                        label = { Text("Fecha límite") },
                        placeholder = { Text("Seleccionar fecha") },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha",
                                tint = Color(0xFF94A3B8)
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
                    onSave(name, deadline.ifBlank { null }, iconName.ifBlank { "⭐" })
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
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