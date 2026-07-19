package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.savings.model.GoalCategory
import fintrack.proyecto4.savings.model.GoalColor
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.theme.FinTrackColors

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        targetAmount: String,
        deadline: String?,
        iconName: String,
        category: GoalCategory,
        colorName: GoalColor,
        priority: GoalPriority,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var iconName by remember { mutableStateOf("⭐") }
    var category by remember { mutableStateOf(GoalCategory.OTHER) }
    var colorName by remember { mutableStateOf(GoalColor.GREEN) }
    var priority by remember { mutableStateOf(GoalPriority.MEDIUM) }
    var notes by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FinTrackColors.TextPrimary,
        unfocusedTextColor = FinTrackColors.TextPrimary,
        focusedLabelColor = FinTrackColors.GreenPrimary,
        unfocusedLabelColor = FinTrackColors.TextSecondary,
        focusedPlaceholderColor = FinTrackColors.TextSecondary,
        unfocusedPlaceholderColor = FinTrackColors.TextSecondary,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedBorderColor = FinTrackColors.GreenPrimary,
        unfocusedBorderColor = FinTrackColors.BorderDefault
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinTrackColors.SurfacePrimary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Nueva meta",
                color = FinTrackColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(50)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Nombre")
                    },
                    placeholder = {
                        Text("Ej: Comprar computadora")
                    },
                    singleLine = true,
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { newValue ->
                        targetAmount = newValue.filter {
                            it.isDigit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Monto objetivo")
                    },
                    placeholder = {
                        Text("Ej: 400000")
                    },
                    prefix = {
                        Text(
                            text = "₡",
                            color = FinTrackColors.TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = fieldColors
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Fecha límite opcional")
                        },
                        placeholder = {
                            Text("Seleccionar fecha")
                        },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    Icons.Default.CalendarToday,
                                contentDescription =
                                    "Seleccionar fecha",
                                tint = FinTrackColors.GreenPrimary
                            )
                        },
                        colors = fieldColors
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                                indication = null,
                                onClick = {
                                    showDatePicker = true
                                }
                            )
                    )
                }

                OutlinedTextField(
                    value = iconName,
                    onValueChange = {
                        iconName = it.take(2)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Ícono")
                    },
                    placeholder = {
                        Text("Ej: 💻")
                    },
                    singleLine = true,
                    colors = fieldColors
                )

                DialogSectionTitle(
                    text = "Categoría"
                )

                CategorySelector(
                    selectedCategory = category,
                    onCategorySelected = {
                        category = it

                        if (
                            iconName.isBlank() ||
                            iconName == "⭐"
                        ) {
                            iconName = categoryIcon(it)
                        }
                    }
                )

                DialogSectionTitle(
                    text = "Prioridad"
                )

                PrioritySelector(
                    selectedPriority = priority,
                    onPrioritySelected = {
                        priority = it
                    }
                )

                DialogSectionTitle(
                    text = "Color"
                )

                ColorSelector(
                    selectedColor = colorName,
                    onColorSelected = {
                        colorName = it
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it.take(250)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    label = {
                        Text("Notas opcionales")
                    },
                    placeholder = {
                        Text(
                            "Agrega detalles importantes sobre esta meta."
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text(
                            text = "${notes.length}/250",
                            color = FinTrackColors.TextSecondary
                        )
                    },
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
                        iconName.ifBlank { "⭐" },
                        category,
                        colorName,
                        priority,
                        notes
                    )
                },
                enabled = name.isNotBlank() &&
                        targetAmount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = FinTrackColors.White,
                    disabledContainerColor =
                        FinTrackColors.SurfaceSecondary,
                    disabledContentColor =
                        FinTrackColors.TextSecondary
                ),
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
                    color = FinTrackColors.TextSecondary
                )
            }
        }
    )

    if (showDatePicker) {
        SavingsDatePickerDialog(
            onDismiss = {
                showDatePicker = false
            },
            onDateSelected = { selectedDate ->
                deadline = selectedDate
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun DialogSectionTitle(
    text: String
) {
    Text(
        text = text,
        color = FinTrackColors.TextPrimary,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun CategorySelector(
    selectedCategory: GoalCategory,
    onCategorySelected: (GoalCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(category)
                },
                label = {
                    Text(
                        text = "${categoryIcon(category)} " +
                                categoryLabel(category)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor =
                        FinTrackColors.SurfaceSecondary,
                    labelColor =
                        FinTrackColors.TextSecondary,
                    selectedContainerColor =
                        FinTrackColors.GreenPrimary,
                    selectedLabelColor =
                        FinTrackColors.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    borderColor =
                        FinTrackColors.BorderDefault,
                    selectedBorderColor =
                        FinTrackColors.GreenPrimary
                )
            )
        }
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: GoalPriority,
    onPrioritySelected: (GoalPriority) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GoalPriority.entries.forEach { priority ->
            FilterChip(
                selected = selectedPriority == priority,
                onClick = {
                    onPrioritySelected(priority)
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        text = priorityLabel(priority)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor =
                        FinTrackColors.SurfaceSecondary,
                    labelColor =
                        FinTrackColors.TextSecondary,
                    selectedContainerColor =
                        FinTrackColors.GreenPrimary,
                    selectedLabelColor =
                        FinTrackColors.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedPriority == priority,
                    borderColor =
                        FinTrackColors.BorderDefault,
                    selectedBorderColor =
                        FinTrackColors.GreenPrimary
                )
            )
        }
    }
}

@Composable
private fun ColorSelector(
    selectedColor: GoalColor,
    onColorSelected: (GoalColor) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalColor.entries.forEach { goalColor ->
            val color = goalColorValue(goalColor)

            Box(
                modifier = Modifier
                    .size(
                        if (selectedColor == goalColor) {
                            40.dp
                        } else {
                            34.dp
                        }
                    )
                    .clip(CircleShape)
                    .background(color)
                    .clickable {
                        onColorSelected(goalColor)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == goalColor) {
                    Text(
                        text = "✓",
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun categoryLabel(
    category: GoalCategory
): String {
    return when (category) {
        GoalCategory.HOME -> "Hogar"
        GoalCategory.VEHICLE -> "Vehículo"
        GoalCategory.TRAVEL -> "Viaje"
        GoalCategory.EDUCATION -> "Educación"
        GoalCategory.TECHNOLOGY -> "Tecnología"
        GoalCategory.EMERGENCY -> "Emergencia"
        GoalCategory.HEALTH -> "Salud"
        GoalCategory.OTHER -> "Otro"
    }
}

private fun categoryIcon(
    category: GoalCategory
): String {
    return when (category) {
        GoalCategory.HOME -> "🏠"
        GoalCategory.VEHICLE -> "🚗"
        GoalCategory.TRAVEL -> "✈️"
        GoalCategory.EDUCATION -> "🎓"
        GoalCategory.TECHNOLOGY -> "💻"
        GoalCategory.EMERGENCY -> "🛡️"
        GoalCategory.HEALTH -> "❤️"
        GoalCategory.OTHER -> "⭐"
    }
}

private fun priorityLabel(
    priority: GoalPriority
): String {
    return when (priority) {
        GoalPriority.LOW -> "Baja"
        GoalPriority.MEDIUM -> "Media"
        GoalPriority.HIGH -> "Alta"
    }
}

private fun goalColorValue(
    color: GoalColor
): Color {
    return when (color) {
        GoalColor.GREEN ->
            FinTrackColors.GreenPrimary

        GoalColor.BLUE ->
            FinTrackColors.BlueMeta

        GoalColor.ORANGE ->
            FinTrackColors.WarningColor

        GoalColor.PURPLE ->
            FinTrackColors.VioletLight

        GoalColor.RED ->
            FinTrackColors.ErrorColor
    }
}