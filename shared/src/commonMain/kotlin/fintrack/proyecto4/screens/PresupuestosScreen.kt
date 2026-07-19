package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.budget.BudgetItem
import fintrack.proyecto4.budget.BudgetListState
import fintrack.proyecto4.budget.BudgetListViewModel
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.BudgetStatus
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.util.formatColones

@Composable
fun PresupuestosScreen(
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onNuevoPresupuesto: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) { BudgetListViewModel(budgetRepository, uid) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedBudget by remember { mutableStateOf<BudgetItem?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showDeactivate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadBudgets()
    }

    Scaffold(
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoPresupuesto,
                containerColor = FinTrackColors.GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo presupuesto",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Presupuestos",
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp)
            )

            Spacer(Modifier.height(20.dp))

            if (!state.isLoading) {
                if (state.budgets.isEmpty()) {
                    EmptyBudgetState(modifier = Modifier.weight(1f))
                } else {
                    SummaryRow(state)
                    Spacer(Modifier.height(20.dp))
                    BudgetList(
                        budgets = state.budgets,
                        onBudgetClick = { selectedBudget = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    val current = selectedBudget
    if (current != null && !showEdit && !showDeactivate) {
        BudgetActionsDialog(
            budget = current,
            onDismiss = { selectedBudget = null },
            onEdit = { showEdit = true },
            onDeactivate = { showDeactivate = true }
        )
    }

    if (current != null && showEdit) {
        EditBudgetDialog(
            budget = current,
            onDismiss = { showEdit = false; selectedBudget = null },
            onSave = { newLimit, newThreshold ->
                viewModel.updateBudget(current.id, newLimit, newThreshold)
                showEdit = false
                selectedBudget = null
            }
        )
    }

    if (current != null && showDeactivate) {
        DeactivateBudgetDialog(
            budget = current,
            onDismiss = { showDeactivate = false; selectedBudget = null },
            onConfirm = {
                viewModel.deactivateBudget(current.id)
                showDeactivate = false
                selectedBudget = null
            }
        )
    }
}

// ── Resumen total ──────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(state: BudgetListState) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            label = "Límite total",
            amount = state.totalLimit,
            amountColor = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Gastado",
            amount = state.totalSpent,
            amountColor = FinTrackColors.ErrorColor,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Disponible",
            amount = state.totalAvailable,
            amountColor = FinTrackColors.GreenPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatColones(amount.toLong()),
            color = amountColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Lista de tarjetas ──────────────────────────────────────────────────────

@Composable
private fun BudgetList(
    budgets: List<BudgetItem>,
    onBudgetClick: (BudgetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(budgets, key = { it.id }) { budget ->
            BudgetCard(budget, onClick = { onBudgetClick(budget) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun BudgetCard(budget: BudgetItem, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val progressColor = when (budget.status) {
        BudgetStatus.EXCEEDED -> FinTrackColors.ErrorColor
        BudgetStatus.CRITICAL -> FinTrackColors.ErrorColor
        BudgetStatus.WARNING  -> FinTrackColors.WarningColor
        BudgetStatus.OK       -> FinTrackColors.GreenPrimary
    }
    val statusLabel = when (budget.status) {
        BudgetStatus.EXCEEDED -> "Excedido"
        BudgetStatus.CRITICAL -> "Cerca del límite"
        BudgetStatus.WARNING  -> "⚠ Alerta"
        BudgetStatus.OK       -> "OK"
    }
    val statusColor = when (budget.status) {
        BudgetStatus.EXCEEDED -> FinTrackColors.ErrorColor
        BudgetStatus.CRITICAL -> FinTrackColors.ErrorColor
        BudgetStatus.WARNING  -> FinTrackColors.WarningColor
        BudgetStatus.OK       -> FinTrackColors.GreenPrimary
    }
    val usageText = if (budget.status == BudgetStatus.EXCEEDED) "Excedido"
                    else "${budget.usagePercentInt}%"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(budget.categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = budget.categoryIcon, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budget.categoryName,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = budget.period,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatColones(budget.spent.toLong())} de ${formatColones(budget.limit.toLong())}",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = usageText,
                color = progressColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { budget.usagePct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = colors.surfaceSecondary,
            strokeCap = StrokeCap.Round
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (budget.status == BudgetStatus.EXCEEDED) {
                Text(
                    text = "${formatColones((-budget.remaining).toLong())} excedido",
                    color = FinTrackColors.ErrorColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "${formatColones(budget.remaining.toLong())} restante",
                    color = if (budget.status == BudgetStatus.OK) FinTrackColors.GreenPrimary
                            else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Estado vacío ───────────────────────────────────────────────────────────

@Composable
private fun EmptyBudgetState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "💰", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No tienes presupuestos activos",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Crea uno para empezar.",
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}
