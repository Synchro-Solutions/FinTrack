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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.ShimmerText
import fintrack.proyecto4.util.formatColones
import kotlinx.coroutines.launch
import fintrack.proyecto4.ai.SavingsAiService
import fintrack.proyecto4.ai.SavingsPlan
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.savings.model.GoalCategory
import fintrack.proyecto4.savings.model.GoalColor
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.savings.ui.GenerateSavingsPlanDialog
import fintrack.proyecto4.savings.ui.SavingsPlanResultDialog
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private data class PendingGoalData(
    val name: String,
    val amount: String,
    val deadline: String?,
    val icon: String,
    val category: GoalCategory,
    val colorName: GoalColor,
    val priority: GoalPriority,
    val notes: String
)

@Composable
fun MetasScreen(
    transactionRepository: TransactionRepository =
        NoOpTransactionRepository(),
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val viewModel = remember { SavingsViewModel() }
    val savingsAiService = remember { SavingsAiService() }
    val scope = rememberCoroutineScope()

    val uid = AuthClient.currentUserId() ?: ""

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var selectedGoal by remember {
        mutableStateOf<SavingsGoal?>(null)
    }

    var detailGoal by remember {
        mutableStateOf<SavingsGoal?>(null)
    }

    var editGoal by remember {
        mutableStateOf<SavingsGoal?>(null)
    }

    var completedGoal by remember {
        mutableStateOf<SavingsGoal?>(null)
    }

    /*
     * Estados relacionados con el plan generado por IA.
     */
    var pendingGoal by remember {
        mutableStateOf<PendingGoalData?>(null)
    }

    var showGeneratePlanDialog by remember {
        mutableStateOf(false)
    }

    var generatedPlan by remember {
        mutableStateOf<SavingsPlan?>(null)
    }

    var isGeneratingPlan by remember {
        mutableStateOf(false)
    }

    var aiErrorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }

    val activeCount = viewModel.activeGoals.size
    val maxGoals = 10
    val canCreateGoal = activeCount < maxGoals

    /*
     * Guarda definitivamente la meta pendiente.
     */
    suspend fun savePendingGoal(): Boolean {
        val goal = pendingGoal ?: return false

        val saved = viewModel.createGoal(
            name = goal.name,
            targetAmountText = goal.amount,
            deadline = goal.deadline,
            iconName = goal.icon,
            category = goal.category,
            colorName = goal.colorName,
            priority = goal.priority,
            notes = goal.notes,
            savingsPlan = generatedPlan
        )

        if (saved) {
            pendingGoal = null
            generatedPlan = null
            aiErrorMessage = null
            showGeneratePlanDialog = false
        }

        return saved
    }

    /*
     * Genera el plan utilizando las transacciones reales del usuario.
     */
    suspend fun generatePlan() {
        val goal = pendingGoal ?: return

        val targetAmount = goal.amount.toDoubleOrNull()

        if (targetAmount == null || targetAmount <= 0.0) {
            aiErrorMessage =
                "El monto objetivo debe ser mayor que cero."
            return
        }

        isGeneratingPlan = true
        aiErrorMessage = null

        try {
            val transactions =
                transactionRepository.getTransactions(uid)

            val totalIncome = transactions
                .filter {
                    it.type == TransactionType.INCOME
                }
                .sumOf {
                    it.amount
                }

            val totalExpenses = transactions
                .filter {
                    it.type == TransactionType.EXPENSE
                }
                .sumOf {
                    it.amount
                }

            val expensesByCategory = transactions
                .filter {
                    it.type == TransactionType.EXPENSE
                }
                .groupBy {
                    it.category
                }
                .mapValues { entry ->
                    entry.value.sumOf {
                        it.amount
                    }
                }

            val requiredMonthlySaving =
                calculateRequiredMonthlySaving(
                    targetAmount = targetAmount,
                    deadline = goal.deadline
                )

            val result =
                savingsAiService.generateSavingsPlan(
                    goalName = goal.name,
                    targetAmount = targetAmount,
                    deadline = goal.deadline,
                    requiredMonthlySaving =
                        requiredMonthlySaving,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    expensesByCategory =
                        expensesByCategory
                )

            result
                .onSuccess { plan ->
                    generatedPlan = plan
                    showGeneratePlanDialog = false
                }
                .onFailure { error ->
                    aiErrorMessage =
                        error.message
                            ?: "No fue posible generar el plan."
                }
        } catch (e: Exception) {
            aiErrorMessage =
                e.message
                    ?: "No fue posible analizar tus movimientos."
        } finally {
            isGeneratingPlan = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack)
                )
            }
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
                    activeGoals =
                        viewModel.activeGoals.size,
                    completedGoals =
                        viewModel.completedGoals.size,
                    averageProgress =
                        viewModel.averageProgress
                )
            }

            item {
                GoalFilters(
                    selectedFilter =
                        viewModel.selectedFilter,
                    onFilterSelected =
                        viewModel::selectFilter
                )
            }

            item {
                GoalSortSelector(
                    selectedSort =
                        viewModel.selectedSort,
                    onSortSelected =
                        viewModel::selectSort
                )
            }

            when {
                viewModel.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color =
                                    FinTrackColors.GreenPrimary
                            )
                        }
                    }
                }

                viewModel.goals.isEmpty() -> {
                    item {
                        EmptyGoalsMessage(
                            title =
                                "Crea tu primera meta",
                            description =
                                "Empieza a ahorrar para cumplir tus objetivos."
                        )
                    }
                }

                viewModel.visibleGoals.isEmpty() -> {
                    item {
                        EmptyGoalsMessage(
                            title = "No hay metas",
                            description =
                                emptyMessageForFilter(
                                    viewModel.selectedFilter
                                )
                        )
                    }
                }

                else -> {
                    items(
                        items =
                            viewModel.visibleGoals,
                        key = {
                            it.id
                        }
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

    /*
     * Formulario para crear la meta.
     *
     * Al presionar Guardar todavía no se almacena.
     * Primero se pregunta si desea generar un plan.
     */
    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = {
                showCreateDialog = false
                pendingGoal = null
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

                pendingGoal = PendingGoalData(
                    name = name,
                    amount = amount,
                    deadline = deadline,
                    icon = icon,
                    category = category,
                    colorName = colorName,
                    priority = priority,
                    notes = notes
                )

                showCreateDialog = false
                showGeneratePlanDialog = true
                generatedPlan = null
                aiErrorMessage = null
            }
        )
    }

    /*
     * Pregunta si se desea generar el plan.
     */
    if (
        showGeneratePlanDialog &&
        pendingGoal != null
    ) {
        GenerateSavingsPlanDialog(
            isLoading = isGeneratingPlan,
            errorMessage = aiErrorMessage,
            onGeneratePlan = {
                scope.launch {
                    generatePlan()
                }
            },
            onSkip = {
                scope.launch {
                    savePendingGoal()
                }
            },
            onDismiss = {
                if (!isGeneratingPlan) {
                    showGeneratePlanDialog = false
                    pendingGoal = null
                    aiErrorMessage = null
                }
            }
        )
    }

    /*
     * Resultado generado por la IA.
     */
    generatedPlan?.let { plan ->
        SavingsPlanResultDialog(
            plan = plan,
            onAccept = {
                scope.launch {
                    savePendingGoal()
                }
            },
            onRegenerate = {
                generatedPlan = null
                showGeneratePlanDialog = true
                aiErrorMessage = null

                scope.launch {
                    generatePlan()
                }
            },
            onCancel = {
                generatedPlan = null
                pendingGoal = null
                aiErrorMessage = null
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
                    val saved =
                        viewModel.addContribution(
                            goalId = goal.id,
                            amountText = amount
                        )

                    if (saved) {
                        selectedGoal = null

                        val updatedGoal =
                            viewModel.goals
                                .firstOrNull {
                                    it.id == goal.id
                                }

                        if (
                            updatedGoal?.status ==
                            GoalStatus.COMPLETED
                        ) {
                            completedGoal =
                                updatedGoal
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
        val currentGoal =
            viewModel.goals
                .firstOrNull {
                    it.id == selectedDetailGoal.id
                }
                ?: selectedDetailGoal

        GoalDetailDialog(
            goal = currentGoal,

            contributions =
                viewModel.getContributions(
                    currentGoal.id
                ),

            projection =
                viewModel.getProjectionForGoal(
                    currentGoal.id
                ),

            isGeneratingProjection =
                viewModel.isGeneratingProjection,

            projectionError =
                viewModel.projectionError,

            onGenerateProjection = {
                scope.launch {
                    viewModel.generateProjection(
                        currentGoal
                    )
                }
            },

            onDismiss = {
                detailGoal = null
                viewModel.clearProjection()
            },

            onCancelGoal = { goal ->
                scope.launch {
                    val cancelled =
                        viewModel.cancelGoal(
                            goal.id
                        )

                    if (cancelled) {
                        detailGoal = null
                        viewModel.clearProjection()
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
                    val updated =
                        viewModel.updateGoal(
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
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Validación",
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = message,
                    color = colors.textPrimary
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
                        color = colors.primary
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
    val colors = LocalAppColors.current
    Column {
        ShimmerText(
            text = "Mis metas",
            baseColor = colors.textPrimary,
            accentColor = colors.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$activeCount / $maxGoals metas activas",
            color = if (canCreateGoal) {
                colors.primary
            } else {
                FinTrackColors.ErrorColor
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Crea objetivos de ahorro y registra tus avances.",
            color = colors.textSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onCreateGoal,
            enabled = canCreateGoal,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryDark,
                contentColor = Color.White,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.textSecondary
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
    val colors = LocalAppColors.current
    Surface(
        modifier = modifier,
        color = colors.surface,
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
                color = colors.textSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = value,
                color = colors.textPrimary,
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
    val colors = LocalAppColors.current
    Column {
        Text(
            text = "Filtrar metas",
            color = colors.textPrimary,
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
                        containerColor = colors.surface,
                        labelColor = colors.textSecondary,
                        selectedContainerColor = colors.primaryDark,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = colors.border,
                        selectedBorderColor = colors.primaryDark
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
    val colors = LocalAppColors.current
    var expanded by remember {
        mutableStateOf(false)
    }

    Column {
        Text(
            text = "Ordenar por",
            color = colors.textPrimary,
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
                color = colors.surface,
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
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Seleccionar orden",
                        tint = colors.textSecondary
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .background(colors.surface)
            ) {
                GoalSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sortLabel(sort),
                                color = if (selectedSort == sort) {
                                    colors.primary
                                } else {
                                    colors.textPrimary
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
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
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
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = colors.textSecondary,
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

private fun formatMoney(amount: Double): String = formatColones(amount)

private fun calculateRequiredMonthlySaving(
    targetAmount: Double,
    deadline: String?
): Double {
    if (deadline.isNullOrBlank()) {
        return targetAmount
    }

    return try {
        val parts = deadline.split("/")

        if (parts.size != 3) {
            return targetAmount
        }

        val deadlineDate = LocalDate(
            year = parts[2].toInt(),
            monthNumber = parts[1].toInt(),
            dayOfMonth = parts[0].toInt()
        )

        val today = Clock.System.todayIn(
            TimeZone.currentSystemDefault()
        )

        val remainingDays =
            deadlineDate.toEpochDays() -
                    today.toEpochDays()

        if (remainingDays <= 0) {
            targetAmount
        } else {
            val remainingMonths =
                ((remainingDays + 29) / 30)
                    .coerceAtLeast(1)

            targetAmount / remainingMonths
        }
    } catch (_: Exception) {
        targetAmount
    }
}
