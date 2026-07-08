package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
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
import fintrack.proyecto4.util.formatColones

@Composable
fun PresupuestosScreen(
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onNuevoPresupuesto: () -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) { BudgetListViewModel(budgetRepository, uid) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FinTrackColors.BgApp,
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
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Presupuestos",
                color = FinTrackColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            if (!state.isLoading) {
                if (state.budgets.isEmpty()) {
                    EmptyBudgetState(modifier = Modifier.weight(1f))
                } else {
                    SummaryRow(state)
                    Spacer(Modifier.height(20.dp))
                    BudgetList(state.budgets, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Resumen total ──────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(state: BudgetListState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            label = "Límite total",
            amount = state.totalLimit,
            amountColor = FinTrackColors.TextPrimary,
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FinTrackColors.SurfacePrimary)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = FinTrackColors.TextSecondary,
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
private fun BudgetList(budgets: List<BudgetItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(budgets, key = { it.id }) { budget ->
            BudgetCard(budget)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun BudgetCard(budget: BudgetItem) {
    val progressColor = when (budget.status) {
        BudgetStatus.CRITICAL -> FinTrackColors.ErrorColor
        BudgetStatus.WARNING  -> FinTrackColors.WarningColor
        BudgetStatus.OK       -> FinTrackColors.GreenPrimary
    }
    val statusLabel = when (budget.status) {
        BudgetStatus.CRITICAL -> "⚠ Crítico"
        BudgetStatus.WARNING  -> "⚠ Alerta"
        BudgetStatus.OK       -> "OK"
    }
    val statusColor = when (budget.status) {
        BudgetStatus.CRITICAL -> FinTrackColors.ErrorColor
        BudgetStatus.WARNING  -> FinTrackColors.WarningColor
        BudgetStatus.OK       -> FinTrackColors.GreenPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinTrackColors.SurfacePrimary)
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
                    color = FinTrackColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = budget.period,
                    color = FinTrackColors.TextSecondary,
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

        LinearProgressIndicator(
            progress = { budget.usagePct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = FinTrackColors.SurfaceSecondary,
            strokeCap = StrokeCap.Round
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatColones(budget.spent.toLong())} gastados",
                color = FinTrackColors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "${formatColones(budget.remaining.toLong())} restante",
                color = if (budget.status == BudgetStatus.OK) FinTrackColors.GreenPrimary
                        else FinTrackColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Estado vacío ───────────────────────────────────────────────────────────

@Composable
private fun EmptyBudgetState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "💰", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Sin presupuestos aún",
            color = FinTrackColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No tienes presupuestos.\nToca + para crear uno.",
            color = FinTrackColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}
