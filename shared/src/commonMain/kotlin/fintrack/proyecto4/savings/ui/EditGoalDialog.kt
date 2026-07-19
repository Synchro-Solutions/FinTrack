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
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.FinTrackColors

@Composable
fun EditGoalDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        deadline: String?,
        iconName: String,
        category: GoalCategory,
        colorName: GoalColor,
        priority: GoalPriority,
        notes: String
    ) -> Unit
) {
    var name by remember(goal.id) {
        mutableStateOf(goal.name)
    }

    var deadline by remember(goal.id) {
        mutableStateOf(goal.deadline ?: "")
    }

    var iconName by remember(goal.id) {
        mutableStateOf(goal.iconName)
    }

    var category by remember(goal.id) {
        mutableStateOf(goal.category)
    }

    var colorName by remember(goal.id) {
        mutableStateOf(goal.colorName)
    }

    var priority by remember(goal.id) {
        mutableStateOf(goal.priority)
    }

    var notes by remember(goal.id) {
        mutableStateOf(goal.notes)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

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
                text = "Editar meta",
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

                if (deadline.isNotBlank()) {
                    TextButton(
                        onClick = {
                            deadline = ""
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Quitar fecha límite",
                            color = FinTrackColors.GreenPrimary
                        )
                    }
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

                EditDialogSectionTitle(
                    text = "Categoría"
                )

                EditCategorySelector(
                    selectedCategory = category,
                    onCategorySelected = { selectedCategory ->
                        category = selectedCategory
                        iconName = editCategoryIcon(selectedCategory)
                    }
                )

                EditDialogSectionTitle(
                    text = "Prioridad"
                )

                EditPrioritySelector(
                    selectedPriority = priority,
                    onPrioritySelected = {
                        priority = it
                    }
                )

                EditDialogSectionTitle(
                    text = "Color"
                )

                EditColorSelector(
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
                        deadline.ifBlank { null },
                        iconName.ifBlank { "⭐" },
                        category,
                        colorName,
                        priority,
                        notes
                    )
                },
                enabled = name.isNotBlank(),
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
private fun EditDialogSectionTitle(
    text: String
) {
    Text(
        text = text,
        color = FinTrackColors.TextPrimary,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun EditCategorySelector(
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
                        text = "${editCategoryIcon(category)} " +
                                editCategoryLabel(category)
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
private fun EditPrioritySelector(
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
                        text = editPriorityLabel(priority)
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
private fun EditColorSelector(
    selectedColor: GoalColor,
    onColorSelected: (GoalColor) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalColor.entries.forEach { goalColor ->
            val displayColor =
                editGoalColorValue(goalColor)

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
                    .background(displayColor)
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

private fun editCategoryLabel(
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

private fun editCategoryIcon(
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

private fun editPriorityLabel(
    priority: GoalPriority
): String {
    return when (priority) {
        GoalPriority.LOW -> "Baja"
        GoalPriority.MEDIUM -> "Media"
        GoalPriority.HIGH -> "Alta"
    }
}

private fun editGoalColorValue(
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