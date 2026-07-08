package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.movimientos.MovimientosFilter
import fintrack.proyecto4.movimientos.MovimientosViewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.util.formatColones

/**
 * Pantalla de historial de movimientos (Sprint 4), siguiendo el wireframe
 * "Pantalla Historial transacciones": buscador, filtros Todos/Ingresos/Gastos y lista.
 * Se conecta a [TransactionRepository][fintrack.proyecto4.transaction.TransactionRepository]
 * a través de [MovimientosViewModel], por lo que refleja automáticamente cualquier
 * transacción guardada desde el formulario manual o el flujo OCR (US-17/US-18).
 */
@Composable
fun MovimientosScreen(
    onAddClick: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {}
) {
    val viewModel = viewModel { MovimientosViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
    ) {
        MovimientosHeader(onAddClick = onAddClick)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SearchField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery
            )

            Spacer(Modifier.height(14.dp))

            FilterRow(
                selected = state.filter,
                onFilterSelected = viewModel::updateFilter
            )
        }

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.transactions.isEmpty() -> LoadingState()
            state.filteredTransactions.isEmpty() -> EmptyMovimientosState(hasAny = state.transactions.isNotEmpty())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(state.filteredTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                    HorizontalDivider(
                        color = FinTrackColors.DividerColor,
                        thickness = 0.5.dp
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MovimientosHeader(onAddClick: () -> Unit) {
    val montserrat = montserratFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Movimientos",
            color = FinTrackColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FinTrackColors.GreenPrimary)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nuevo movimiento",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Buscar movimiento...",
                color = FinTrackColors.TextSecondary,
                fontSize = 14.sp,
                fontFamily = montserratFamily()
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = FinTrackColors.TextSecondary
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = FinTrackColors.TextPrimary),
        colors = formTextFieldColors()
    )
}

@Composable
private fun FilterRow(selected: MovimientosFilter, onFilterSelected: (MovimientosFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableChip(
            text = "Todos",
            selected = selected == MovimientosFilter.TODOS,
            onClick = { onFilterSelected(MovimientosFilter.TODOS) }
        )
        SelectableChip(
            text = "Ingresos",
            selected = selected == MovimientosFilter.INGRESOS,
            onClick = { onFilterSelected(MovimientosFilter.INGRESOS) }
        )
        SelectableChip(
            text = "Gastos",
            selected = selected == MovimientosFilter.GASTOS,
            onClick = { onFilterSelected(MovimientosFilter.GASTOS) }
        )
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val montserrat = montserratFamily()
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) FinTrackColors.GreenPrimary else FinTrackColors.ErrorColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                color = FinTrackColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = montserrat,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(transaction.category)
                    transaction.paymentMethod?.let { append(" · ${it.label}") }
                    append(" · ${transaction.date}")
                },
                color = FinTrackColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = montserrat,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${if (isIncome) "+" else "-"}${formatColones(transaction.amount)}",
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserrat
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FinTrackColors.GreenPrimary)
    }
}

@Composable
private fun EmptyMovimientosState(hasAny: Boolean) {
    val montserrat = montserratFamily()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FinTrackColors.SurfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = FinTrackColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasAny) "Sin resultados" else "Aún no tienes movimientos",
            color = FinTrackColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserrat
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasAny) {
                "Prueba con otra búsqueda o filtro."
            } else {
                "Presiona + para registrar tu primer ingreso o gasto."
            },
            color = FinTrackColors.TextSecondary,
            fontSize = 12.sp,
            fontFamily = montserrat
        )
    }
}
