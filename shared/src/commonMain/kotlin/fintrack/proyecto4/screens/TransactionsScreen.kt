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
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.theme.subtleSurface
import fintrack.proyecto4.transaction.DateScope
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.PaymentMethod
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.transaction.TransactionsFilter
import fintrack.proyecto4.transaction.TransactionsViewModel
import fintrack.proyecto4.util.formatColones

/**
 * Pantalla de historial de movimientos (Sprint 4), siguiendo el wireframe
 * "Pantalla Historial transacciones": buscador, filtros Todos/Ingresos/Gastos y lista.
 * Se conecta a [TransactionRepository] a través de [TransactionsViewModel], por lo que
 * refleja automáticamente cualquier transacción guardada desde el formulario manual o el
 * flujo OCR (US-17/US-18) para el usuario actualmente autenticado.
 */
@Composable
fun TransactionsScreen(
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    onAddClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) { TransactionsViewModel(transactionRepository, uid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        TransactionsHeader(onAddClick = onAddClick)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = viewModel::updateSearchQuery
                    )
                }
                Spacer(Modifier.width(8.dp))
                FiltersButton(
                    activeCount = state.activeFilterCount,
                    onClick = { showFilters = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            FilterRow(
                selected = state.filter,
                onFilterSelected = viewModel::updateFilter
            )
        }

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.transactions.isEmpty() -> LoadingState()
            state.filteredTransactions.isEmpty() -> EmptyTransactionsState(hasAny = state.transactions.isNotEmpty())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(state.pagedTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        searchQuery = state.searchQuery,
                        onClick = { onTransactionClick(transaction) }
                    )
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp
                    )
                }
                if (state.hasMoreToLoad) {
                    item { LoadMoreButton(onClick = viewModel::loadMore) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showFilters) {
        FiltersDialog(
            state = state,
            onDateScopeSelected = viewModel::updateDateScope,
            onCategorySelected = viewModel::updateCategoryFilter,
            onPaymentMethodSelected = viewModel::updatePaymentMethodFilter,
            onClear = viewModel::clearAdvancedFilters,
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun TransactionsHeader(onAddClick: () -> Unit) {
    val colors = LocalAppColors.current
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
            color = colors.textPrimary,
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
    val colors = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Buscar movimiento o monto...",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = montserratFamily()
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
        colors = formTextFieldColors()
    )
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    BadgedBox(
        badge = {
            if (activeCount > 0) {
                Badge(containerColor = FinTrackColors.GreenPrimary) {
                    Text("$activeCount", color = Color.White, fontSize = 9.sp)
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.subtleSurface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filtros",
                tint = colors.textPrimary
            )
        }
    }
}

@Composable
private fun FilterRow(selected: TransactionsFilter, onFilterSelected: (TransactionsFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableChip(
            text = "Todos",
            selected = selected == TransactionsFilter.ALL,
            onClick = { onFilterSelected(TransactionsFilter.ALL) }
        )
        SelectableChip(
            text = "Ingresos",
            selected = selected == TransactionsFilter.INCOME,
            onClick = { onFilterSelected(TransactionsFilter.INCOME) }
        )
        SelectableChip(
            text = "Gastos",
            selected = selected == TransactionsFilter.EXPENSE,
            onClick = { onFilterSelected(TransactionsFilter.EXPENSE) }
        )
    }
}

/**
 * US-13: filtros avanzados (rango de fechas, categoría, método de pago) separados de los
 * chips Todos/Ingresos/Gastos para no saturar la barra principal del historial.
 */
@Composable
private fun FiltersDialog(
    state: fintrack.proyecto4.transaction.TransactionsUiState,
    onDateScopeSelected: (DateScope) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Text(
                text = "Filtros",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )

            Spacer(Modifier.height(18.dp))
            FilterSectionLabel("RANGO DE FECHAS")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    text = "Este mes",
                    selected = state.dateScope == DateScope.CURRENT_MONTH,
                    onClick = { onDateScopeSelected(DateScope.CURRENT_MONTH) }
                )
                SelectableChip(
                    text = "Todo el historial",
                    selected = state.dateScope == DateScope.ALL,
                    onClick = { onDateScopeSelected(DateScope.ALL) }
                )
            }

            if (state.availableCategories.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionLabel("CATEGORÍA")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip(
                        text = "Todas",
                        selected = state.categoryFilter == null,
                        onClick = { onCategorySelected(null) }
                    )
                    state.availableCategories.forEach { category ->
                        SelectableChip(
                            text = category,
                            selected = state.categoryFilter == category,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            FilterSectionLabel("MÉTODO DE PAGO")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    text = "Todos",
                    selected = state.paymentMethodFilter == null,
                    onClick = { onPaymentMethodSelected(null) }
                )
                PaymentMethod.values().forEach { method ->
                    SelectableChip(
                        text = method.label,
                        selected = state.paymentMethodFilter == method,
                        onClick = { onPaymentMethodSelected(method) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClear) {
                    Text("Limpiar filtros", color = colors.textSecondary, fontFamily = montserrat)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
                ) {
                    Text("Cerrar", color = Color.White, fontFamily = montserrat)
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF58708F),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = montserratFamily()
    )
}

@Composable
private fun LoadMoreButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Cargar más",
            color = FinTrackColors.GreenPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserratFamily()
        )
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, searchQuery: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
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
                text = highlightedText(transaction.description, searchQuery),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = montserrat,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                Text(
                    text = highlightedText(transaction.category, searchQuery),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = montserrat,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        transaction.paymentMethod?.let { append(" · ${it.label}") }
                        append(" · ${transaction.date}")
                    },
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = montserrat,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.subtleSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${if (isIncome) "+" else "-"}${formatColones(transaction.amount)}",
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = montserrat
            )
        }
    }
}

/** US-50: resalta la subcadena de [text] que coincide con [query] (búsqueda por texto). */
private fun highlightedText(text: String, query: String): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return AnnotatedString(text)
    val index = text.indexOf(trimmed, ignoreCase = true)
    if (index < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, index))
        withStyle(SpanStyle(color = FinTrackColors.GreenPrimary, fontWeight = FontWeight.Bold)) {
            append(text.substring(index, index + trimmed.length))
        }
        append(text.substring(index + trimmed.length))
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FinTrackColors.GreenPrimary)
    }
}

@Composable
private fun EmptyTransactionsState(hasAny: Boolean) {
    val colors = LocalAppColors.current
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
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasAny) "Sin resultados" else "Aún no tienes movimientos",
            color = colors.textPrimary,
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
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = montserrat
        )
    }
}
