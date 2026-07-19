package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.ui.AddContributionDialog
import fintrack.proyecto4.savings.ui.CreateGoalDialog
import fintrack.proyecto4.savings.ui.EditGoalDialog
import fintrack.proyecto4.savings.ui.GoalCard
import fintrack.proyecto4.savings.ui.GoalCompletedDialog
import fintrack.proyecto4.savings.ui.GoalDetailDialog
import fintrack.proyecto4.savings.viewmodel.GoalFilter
import fintrack.proyecto4.savings.viewmodel.GoalSort
import fintrack.proyecto4.savings.viewmodel.SavingsViewModel
import fintrack.proyecto4.theme.FinTrackColors
import kotlinx.coroutines.launch

@Composable
fun MetasScreen() {
    val viewModel = remember { SavingsViewModel() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var detailGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var editGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var completedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }

    val activeCount = viewModel.activeGoals.size
    val maxGoals = 10
    val canCreateGoal = activeCount < maxGoals

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GoalsHeader(
                    activeCount = activeCount,
                    maxGoals = maxGoals,
                    canCreateGoal = canCreateGoal,
                    onCreateGoal = {
                        if (canCreateGoal) {
                            showCreateDialog = true
                        }
                    }
                )
            }

            item {
                SavingsSummary(
                    totalSaved = viewModel.totalSaved,
                    activeGoals = viewModel.activeGoals.size,
                    completedGoals = viewModel.completedGoals.size,
                    averageProgress = viewModel.averageProgress
                )
            }

            item {
                GoalFilters(
                    selectedFilter = viewModel.selectedFilter,
                    onFilterSelected = viewModel::selectFilter
                )
            }

            item {
                GoalSortSelector(
                    selectedSort = viewModel.selectedSort,
                    onSortSelected = viewModel::selectSort
                )
            }

            when {
                viewModel.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = FinTrackColors.GreenPrimary
                            )
                        }
                    }
                }

                viewModel.goals.isEmpty() -> {
                    item {
                        EmptyGoalsMessage(
                            title = "Crea tu primera meta",
                            description = "Empieza a ahorrar para cumplir tus objetivos."
                        )
                    }
                }

                viewModel.visibleGoals.isEmpty() -> {
                    item {
                        EmptyGoalsMessage(
                            title = "No hay metas",
                            description = emptyMessageForFilter(
                                viewModel.selectedFilter
                            )
                        )
                    }
                }

                else -> {
                    items(
                        items = viewModel.visibleGoals,
                        key = { it.id }
                    ) { goal ->
                        GoalCard(
                            goal = goal,
                            onAddContribution = {
                                selectedGoal = it
                            },
                            onViewDetail = {
                                detailGoal = it
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = {
                showCreateDialog = false
                viewModel.clearError()
            },
            onSave = {
                    name,
                    amount,
                    deadline,
                    icon,
                    category,
                    colorName,
                    priority,
                    notes ->

                scope.launch {
                    val saved = viewModel.createGoal(
                        name = name,
                        targetAmountText = amount,
                        deadline = deadline,
                        iconName = icon,
                        category = category,
                        colorName = colorName,
                        priority = priority,
                        notes = notes
                    )

                    if (saved) {
                        showCreateDialog = false
                    }
                }
            }
        )
    }

    selectedGoal?.let { goal ->
        AddContributionDialog(
            goal = goal,
            onDismiss = {
                selectedGoal = null
                viewModel.clearError()
            },
            onSave = { amount ->
                scope.launch {
                    val saved = viewModel.addContribution(
                        goalId = goal.id,
                        amountText = amount
                    )

                    if (saved) {
                        selectedGoal = null

                        val updatedGoal = viewModel.goals
                            .firstOrNull { it.id == goal.id }

                        if (
                            updatedGoal?.status ==
                            GoalStatus.COMPLETED
                        ) {
                            completedGoal = updatedGoal
                        }
                    }
                }
            }
        )
    }

    completedGoal?.let { goal ->
        GoalCompletedDialog(
            goal = goal,
            onDismiss = {
                completedGoal = null
            }
        )
    }

    detailGoal?.let { selectedDetailGoal ->
        val currentGoal = viewModel.goals
            .firstOrNull { it.id == selectedDetailGoal.id }
            ?: selectedDetailGoal

        GoalDetailDialog(
            goal = currentGoal,
            contributions = viewModel.getContributions(
                currentGoal.id
            ),
            onDismiss = {
                detailGoal = null
            },
            onCancelGoal = { goal ->
                scope.launch {
                    val cancelled = viewModel.cancelGoal(goal.id)

                    if (cancelled) {
                        detailGoal = null
                    }
                }
            },
            onEditGoal = {
                editGoal = it
            }
        )
    }

    editGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = {
                editGoal = null
            },
            onSave = {
                    name,
                    deadline,
                    icon,
                    category,
                    colorName,
                    priority,
                    notes ->

                scope.launch {
                    val updated = viewModel.updateGoal(
                        goalId = goal.id,
                        name = name,
                        deadline = deadline,
                        iconName = icon,
                        category = category,
                        colorName = colorName,
                        priority = priority,
                        notes = notes
                    )

                    if (updated) {
                        editGoal = null
                        detailGoal = null
                    }
                }
            }
        )
    }

    viewModel.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
            },
            containerColor = Color(0xFF111C2E),
            title = {
                Text(
                    text = "Validación",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = message,
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearError()
                    }
                ) {
                    Text(
                        text = "Aceptar",
                        color = Color(0xFF22C55E)
                    )
                }
            }
        )
    }
}

@Composable
private fun GoalsHeader(
    activeCount: Int,
    maxGoals: Int,
    canCreateGoal: Boolean,
    onCreateGoal: () -> Unit
) {
    Column {
        Text(
            text = "Mis metas",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$activeCount / $maxGoals metas activas",
            color = if (canCreateGoal) {
                Color(0xFF22C55E)
            } else {
                Color(0xFFEF4444)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Crea objetivos de ahorro y registra tus avances.",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onCreateGoal,
            enabled = canCreateGoal,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF334155),
                disabledContentColor = Color(0xFF94A3B8)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (canCreateGoal) {
                    "+ Nueva meta"
                } else {
                    "Límite alcanzado"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SavingsSummary(
    totalSaved: Double,
    activeGoals: Int,
    completedGoals: Int,
    averageProgress: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                title = "Ahorrado total",
                value = formatMoney(totalSaved),
                icon = "💰",
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Metas activas",
                value = activeGoals.toString(),
                icon = "🎯",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                title = "Completadas",
                value = completedGoals.toString(),
                icon = "🏆",
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Progreso promedio",
                value = "$averageProgress%",
                icon = "📈",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF111C2E),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = icon,
                fontSize = 21.sp
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoalFilters(
    selectedFilter: GoalFilter,
    onFilterSelected: (GoalFilter) -> Unit
) {
    Column {
        Text(
            text = "Filtrar metas",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        onFilterSelected(filter)
                    },
                    label = {
                        Text(filterLabel(filter))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF111C2E),
                        labelColor = Color(0xFF94A3B8),
                        selectedContainerColor = Color(0xFF22C55E),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = Color(0xFF22C55E)
                    )
                )
            }
        }
    }
}

@Composable
private fun GoalSortSelector(
    selectedSort: GoalSort,
    onSortSelected: (GoalSort) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column {
        Text(
            text = "Ordenar por",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    },
                color = Color(0xFF111C2E),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = sortLabel(selectedSort),
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Seleccionar orden",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .background(Color(0xFF111C2E))
            ) {
                GoalSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sortLabel(sort),
                                color = if (selectedSort == sort) {
                                    Color(0xFF22C55E)
                                } else {
                                    Color.White
                                }
                            )
                        },
                        onClick = {
                            onSortSelected(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsMessage(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111C2E),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 34.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
    }
}

private fun filterLabel(
    filter: GoalFilter
): String {
    return when (filter) {
        GoalFilter.ALL -> "Todas"
        GoalFilter.ACTIVE -> "Activas"
        GoalFilter.COMPLETED -> "Completadas"
        GoalFilter.CANCELLED -> "Canceladas"
    }
}

private fun sortLabel(
    sort: GoalSort
): String {
    return when (sort) {
        GoalSort.MOST_RECENT -> "Más recientes"
        GoalSort.HIGHEST_PROGRESS -> "Mayor progreso"
        GoalSort.LOWEST_PROGRESS -> "Menor progreso"
        GoalSort.DEADLINE -> "Fecha límite"
        GoalSort.NAME -> "Nombre"
    }
}

private fun emptyMessageForFilter(
    filter: GoalFilter
): String {
    return when (filter) {
        GoalFilter.ALL ->
            "Todavía no tienes metas registradas."

        GoalFilter.ACTIVE ->
            "No tienes metas activas."

        GoalFilter.COMPLETED ->
            "Todavía no has completado ninguna meta."

        GoalFilter.CANCELLED ->
            "No tienes metas canceladas."
    }
}

private fun formatMoney(
    amount: Double
): String {
    val cleanAmount = amount.toLong()

    val formatted = cleanAmount
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return "₡$formatted"
}