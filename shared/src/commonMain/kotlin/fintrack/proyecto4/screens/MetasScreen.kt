package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import fintrack.proyecto4.savings.viewmodel.SavingsViewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
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

    val colors = LocalAppColors.current
    val activeCount = viewModel.activeGoals.size
    val maxGoals = 10
    val canCreateGoal = activeCount < maxGoals

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(20.dp)
    ) {
        Column {
            Text("Mis metas", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$activeCount / $maxGoals metas activas",
                color = if (canCreateGoal) FinTrackColors.GreenPrimary else FinTrackColors.ErrorColor,
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
                onClick = { if (canCreateGoal) showCreateDialog = true },
                enabled = canCreateGoal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = colors.surfaceSecondary,
                    disabledContentColor = colors.textSecondary
                )
            ) {
                Text(if (canCreateGoal) "+ Nueva meta" else "Límite alcanzado")
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator(color = FinTrackColors.GreenPrimary)
            } else if (viewModel.activeGoals.isEmpty() && viewModel.completedGoals.isEmpty()) {
                Text(
                    text = "Crea tu primera meta de ahorro.",
                    color = colors.textSecondary,
                    fontSize = 15.sp
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(viewModel.activeGoals) { goal ->
                        GoalCard(
                            goal = goal,
                            onAddContribution = { selectedGoal = it },
                            onViewDetail = { detailGoal = it }
                        )
                    }

                    if (viewModel.completedGoals.isNotEmpty()) {
                        item {
                            Text(
                                text = "Completadas",
                                color = colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        items(viewModel.completedGoals) { goal ->
                            GoalCard(
                                goal = goal,
                                onAddContribution = { selectedGoal = it },
                                onViewDetail = { detailGoal = it }
                            )
                        }
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
            onSave = { name, amount, deadline, icon ->
                scope.launch {
                    val saved = viewModel.createGoal(name, amount, deadline, icon)
                    if (saved) showCreateDialog = false
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
                    val saved = viewModel.addContribution(goal.id, amount)
                    if (saved) {
                        selectedGoal = null
                        val updatedGoal = viewModel.goals.firstOrNull { it.id == goal.id }
                        if (updatedGoal?.status == GoalStatus.COMPLETED) {
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
            onDismiss = { completedGoal = null }
        )
    }

    detailGoal?.let { goal ->
        GoalDetailDialog(
            goal = goal,
            contributions = viewModel.getContributions(goal.id),
            onDismiss = { detailGoal = null },
            onCancelGoal = {
                scope.launch {
                    val cancelled = viewModel.cancelGoal(it.id)
                    if (cancelled) detailGoal = null
                }
            },
            onEditGoal = { editGoal = it }
        )
    }

    editGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { editGoal = null },
            onSave = { name, deadline, icon ->
                scope.launch {
                    val updated = viewModel.updateGoal(
                        goalId = goal.id,
                        name = name,
                        deadline = deadline,
                        iconName = icon
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
            onDismissRequest = { viewModel.clearError() },
            containerColor = colors.surface,
            title = { Text("Validación", color = colors.textPrimary) },
            text = { Text(message, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Aceptar")
                }
            }
        )
    }
}
