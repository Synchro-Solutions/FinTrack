package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import fintrack.proyecto4.transaction.TransactionsUiState
import fintrack.proyecto4.transaction.TransactionsViewModel
import fintrack.proyecto4.util.formatColones

/** Máximo de chips de categoría visibles antes de mostrar "Ver todas". */
private const val VisibleCategoriesCount = 5

/**
 * Pantalla de historial de movimientos (Sprint 4), siguiendo el wireframe
 * "Pantalla Historial transacciones": buscador, filtros Todos/Ingresos/Gastos y lista.
 * Se conecta a [TransactionRepository] a través de [TransactionsViewModel], por lo que
 * refleja automáticamente cualquier transacción guardada desde el formulario manual o el
 * flujo OCR (US-17/US-18) para el usuario actualmente autenticado.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // El movimiento recién creado queda primero en la lista (ordenada por fecha de creación
    // descendente), pero LazyColumn usa `key = { it.id }` para mantener el ítem visible
    // anclado en su posición: si no se fuerza el scroll, al volver del formulario la pantalla
    // se queda "pegada" en el mismo ítem que se veía antes en vez de mostrar el nuevo arriba.
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            listState.scrollToItem(0)
        }
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
                state = listState,
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
        FiltersSheet(
            state = state,
            onApply = { dateScope, customFrom, customTo, category, paymentMethod ->
                viewModel.applyAdvancedFilters(dateScope, customFrom, customTo, category, paymentMethod)
                showFilters = false
            },
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
 * US-13: filtros avanzados (rango de fechas, categoría, método de pago) como bottom sheet,
 * separados de los chips Todos/Ingresos/Gastos para no saturar la barra principal del historial.
 *
 * Los cambios dentro del sheet quedan en estado local ("staged") y solo se aplican al historial
 * cuando el usuario presiona "Aplicar filtros" (ver [onApply]). Cerrar con la "X", tocar fuera
 * del sheet o el botón atrás del sistema descarta esos cambios sin tocar el ViewModel — el
 * usuario siempre puede salir sin alterar los filtros que ya tenía aplicados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    state: TransactionsUiState,
    onApply: (
        dateScope: DateScope,
        customDateFrom: String?,
        customDateTo: String?,
        category: String?,
        paymentMethod: PaymentMethod?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dateScope by remember { mutableStateOf(state.dateScope) }
    var customFrom by remember { mutableStateOf(state.customDateFrom) }
    var customTo by remember { mutableStateOf(state.customDateTo) }
    var category by remember { mutableStateOf(state.categoryFilter) }
    var paymentMethod by remember { mutableStateOf(state.paymentMethodFilter) }
    var showAllCategories by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserrat
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar sin aplicar cambios",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDismiss)
                )
            }

            Spacer(Modifier.height(20.dp))
            FilterSectionLabel("RANGO DE FECHAS")
            Spacer(Modifier.height(10.dp))
            DateRangeGrid(
                selected = dateScope,
                onSelected = { dateScope = it }
            )
            if (dateScope == DateScope.CUSTOM) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DateField(
                            value = customFrom ?: "",
                            onClick = { showFromPicker = true },
                            placeholder = "Desde"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DateField(
                            value = customTo ?: "",
                            onClick = { showToPicker = true },
                            placeholder = "Hasta"
                        )
                    }
                }
            }

            if (state.availableCategories.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                FilterSectionLabel("CATEGORÍA")
                Spacer(Modifier.height(10.dp))
                val categoriesToShow = if (showAllCategories) {
                    state.availableCategories
                } else {
                    state.availableCategories.take(VisibleCategoriesCount)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip(
                        text = "Todas",
                        selected = category == null,
                        onClick = { category = null }
                    )
                    categoriesToShow.forEach { c ->
                        SelectableChip(
                            text = c,
                            selected = category == c,
                            onClick = { category = c }
                        )
                    }
                }
                if (state.availableCategories.size > VisibleCategoriesCount) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clickable { showAllCategories = !showAllCategories },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAllCategories) "Ver menos" else "Ver todas",
                            color = FinTrackColors.GreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = montserrat
                        )
                        Icon(
                            imageVector = if (showAllCategories) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = FinTrackColors.GreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            FilterSectionLabel("MÉTODO DE PAGO")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    text = "Todos",
                    selected = paymentMethod == null,
                    onClick = { paymentMethod = null }
                )
                PaymentMethod.values().forEach { method ->
                    SelectableChip(
                        text = method.label,
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method }
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        dateScope = DateScope.ALL
                        customFrom = null
                        customTo = null
                        category = null
                        paymentMethod = null
                        showAllCategories = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                ) {
                    Text(
                        text = "Limpiar filtros",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                }
                Button(
                    onClick = { onApply(dateScope, customFrom, customTo, category, paymentMethod) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
                ) {
                    Text(
                        text = "Aplicar filtros",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                }
            }
        }
    }

    if (showFromPicker) {
        FintrackDatePickerDialog(
            initialDateMillis = parseDateToEpochMillis(customFrom ?: ""),
            onDismissRequest = { showFromPicker = false },
            onDateSelected = { millis -> customFrom = formatEpochMillisToDate(millis) }
        )
    }

    if (showToPicker) {
        FintrackDatePickerDialog(
            initialDateMillis = parseDateToEpochMillis(customTo ?: ""),
            onDismissRequest = { showToPicker = false },
            onDateSelected = { millis -> customTo = formatEpochMillisToDate(millis) }
        )
    }
}

@Composable
private fun DateRangeGrid(selected: DateScope, onSelected: (DateScope) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateScopeButton(
                text = "Este mes",
                selected = selected == DateScope.CURRENT_MONTH,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(DateScope.CURRENT_MONTH) }
            )
            DateScopeButton(
                text = "Últimos 3 meses",
                selected = selected == DateScope.LAST_3_MONTHS,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(DateScope.LAST_3_MONTHS) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateScopeButton(
                text = "Este año",
                selected = selected == DateScope.CURRENT_YEAR,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(DateScope.CURRENT_YEAR) }
            )
            DateScopeButton(
                text = "Todo el historial",
                selected = selected == DateScope.ALL,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(DateScope.ALL) }
            )
        }
        DateScopeButton(
            text = "Personalizado",
            selected = selected == DateScope.CUSTOM,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.CalendarToday,
            onClick = { onSelected(DateScope.CUSTOM) }
        )
    }
}

@Composable
private fun DateScopeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) FinTrackColors.GreenPrimary else colors.subtleSurface)
            .border(
                width = 1.dp,
                color = if (selected) FinTrackColors.GreenPrimary else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = if (icon != null) Arrangement.SpaceBetween else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat
        )
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (selected) Color.White else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
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
