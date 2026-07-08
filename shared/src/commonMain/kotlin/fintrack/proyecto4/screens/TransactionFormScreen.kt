package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.screens.common.SuccessSnackbarHost
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LightAppColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.theme.subtleSurface
import fintrack.proyecto4.transaction.MaxDescriptionLength
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.PaymentMethod
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionFormViewModel
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Cuánto se muestra el Snackbar de éxito antes de navegar fuera de la pantalla. */
private const val SuccessSnackbarDelayMillis = 900L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    initialType: TransactionType = TransactionType.EXPENSE,
    editingTransaction: Transaction? = null,
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onOcrClick: () -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    // remember (no viewModel(key=...)) a propósito: cada visita a esta pantalla debe partir
    // de un formulario en blanco (o precargado solo con la transacción a editar). Con
    // viewModel(key=...) el ViewModelStore de la Activity reutilizaba la misma instancia
    // entre visitas consecutivas de "nueva transacción" (misma key), dejando los campos de
    // la transacción anterior visibles al crear una nueva.
    val viewModel = remember(uid, editingTransaction?.id, initialType) {
        TransactionFormViewModel(transactionRepository, uid, initialType, editingTransaction)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        TransactionHeader(
            title = if (viewModel.isEditing) "Editar movimiento" else "Nuevo movimiento",
            onBack = onBack,
            onOcrClick = onOcrClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 24.dp)
        ) {
            TransactionTypeTabs(
                selectedType = state.type,
                // US-14: el tipo (Ingreso/Gasto) no se puede cambiar tras guardar la
                // transacción, así que en modo edición el selector queda bloqueado.
                locked = viewModel.isEditing,
                onTypeSelected = viewModel::changeType
            )

            Spacer(Modifier.height(18.dp))

            FormLabel("MONTO (₡) *")
            AmountField(
                value = state.amount,
                onValueChange = viewModel::updateAmount
            )

            Spacer(Modifier.height(18.dp))

            FormLabel("DESCRIPCIÓN *")
            DescriptionField(
                value = state.description,
                onValueChange = viewModel::updateDescription
            )

            Spacer(Modifier.height(18.dp))

            FormLabel("CATEGORÍA *")
            CategorySection(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = viewModel::selectCategory
            )

            Spacer(Modifier.height(18.dp))

            FormLabel("MÉTODO DE PAGO")
            PaymentMethodSection(
                selectedPaymentMethod = state.paymentMethod,
                onPaymentMethodSelected = viewModel::selectPaymentMethod
            )

            Spacer(Modifier.height(18.dp))

            FormLabel("FECHA *")
            DateField(
                value = state.date,
                onClick = { showDatePicker = true }
            )

            if (saveError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = saveError ?: "",
                    color = FinTrackColors.ErrorColor,
                    fontSize = 12.sp,
                    fontFamily = montserratFamily()
                )
            }

            Spacer(Modifier.height(22.dp))

            SaveTransactionButton(
                type = state.type,
                editing = viewModel.isEditing,
                enabled = state.isValid && !isSaving,
                isSaving = isSaving,
                onClick = {
                    val editing = viewModel.isEditing
                    viewModel.saveTransaction {
                        scope.launch {
                            launch {
                                snackbarHostState.showSnackbar(
                                    message = if (editing) {
                                        "Movimiento actualizado exitosamente"
                                    } else {
                                        "Movimiento registrado exitosamente"
                                    },
                                    duration = SnackbarDuration.Short
                                )
                            }
                            delay(SuccessSnackbarDelayMillis)
                            onSaved()
                        }
                    }
                }
            )
        }
    }

    SuccessSnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToEpochMillis(state.date),
            selectableDates = NotFutureSelectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = fintrackDatePickerColors(),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.updateDate(formatEpochMillisToDate(millis))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = FinTrackColors.GreenPrimary
                    )
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.textSecondary
                    )
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = fintrackDatePickerColors()
            )
        }
    }
}

/**
 * El calendario se muestra siempre como una tarjeta blanca, sin importar el tema activo de
 * la app (igual que en muchos selectores de fecha nativos). Por eso TODOS los colores de
 * contenido se fijan explícitamente a los de [LightAppColors] en vez de dejarlos heredar del
 * MaterialTheme: si no se fijan, en modo oscuro el color de texto por defecto es claro
 * (pensado para fondos oscuros) y queda casi invisible sobre esta tarjeta blanca.
 */
@Composable
internal fun fintrackDatePickerColors() = DatePickerDefaults.colors(
    containerColor = Color.White,
    titleContentColor = LightAppColors.textSecondary,
    headlineContentColor = LightAppColors.textPrimary,
    weekdayContentColor = LightAppColors.textSecondary,
    subheadContentColor = LightAppColors.textPrimary,
    yearContentColor = LightAppColors.textPrimary,
    dayContentColor = LightAppColors.textPrimary,
    disabledDayContentColor = LightAppColors.textSecondary.copy(alpha = 0.4f),
    dividerColor = LightAppColors.divider,
    todayContentColor = FinTrackColors.GreenPrimary,
    todayDateBorderColor = FinTrackColors.GreenPrimary,
    selectedDayContainerColor = FinTrackColors.GreenPrimary,
    selectedDayContentColor = Color.White,
    selectedYearContainerColor = FinTrackColors.GreenPrimary,
    selectedYearContentColor = Color.White,
    currentYearContentColor = FinTrackColors.GreenPrimary,
    navigationContentColor = FinTrackColors.GreenPrimary
)

internal fun formatEpochMillisToDate(millis: Long): String {
    val date = LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

internal fun parseDateToEpochMillis(value: String): Long? {
    val parts = value.split("/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return try {
        LocalDate(year, month, day).toEpochDays() * 86_400_000L
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * US-14/US-11/US-12/US-18: ninguna transacción (manual u OCR) puede tener fecha futura.
 * Se aplica directamente en el DatePicker (en vez de solo validar después) para que ni
 * siquiera sea posible seleccionar un día inválido.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal val NotFutureSelectableDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= todayEndOfDayMillis()
}

private fun todayEndOfDayMillis(): Long {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return (today.toEpochDays() + 1) * 86_400_000L - 1
}

@Composable
private fun TransactionHeader(title: String, onBack: () -> Unit, onOcrClick: () -> Unit) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            tint = colors.textPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack)
        )

        Spacer(Modifier.width(18.dp))

        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Escanear con OCR",
            tint = colors.textPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onOcrClick)
        )
    }
}

@Composable
private fun TransactionTypeTabs(
    selectedType: TransactionType,
    locked: Boolean = false,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE1E7F0))
            .alpha(if (locked) 0.6f else 1f)
    ) {
        TransactionTypeTab(
            text = "Gasto",
            selected = selectedType == TransactionType.EXPENSE,
            selectedColor = Color(0xFFE53935),
            enabled = !locked,
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected(TransactionType.EXPENSE) }
        )

        TransactionTypeTab(
            text = "Ingreso",
            selected = selectedType == TransactionType.INCOME,
            selectedColor = FinTrackColors.GreenPrimary,
            enabled = !locked,
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected(TransactionType.INCOME) }
        )
    }
}

@Composable
private fun TransactionTypeTab(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val montserrat = montserratFamily()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(1.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.2.dp,
                        color = Color(0xFF111827),
                        shape = RoundedCornerShape(15.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) selectedColor else Color(0xFF64748B),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat
        )
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF58708F),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = montserratFamily()
    )
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "0",
                color = colors.textSecondary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(top = 6.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = formTextFieldColors()
    )
}

@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = { onValueChange(it.take(MaxDescriptionLength)) },
        placeholder = {
            Text(
                text = "Ej. Supermercado, Salario...",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = montserratFamily()
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 6.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            color = colors.textPrimary,
            fontFamily = montserratFamily()
        ),
        singleLine = true,
        colors = formTextFieldColors()
    )
}

@Composable
private fun CategorySection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            SelectableChip(
                text = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
internal fun PaymentMethodSection(
    selectedPaymentMethod: PaymentMethod?,
    onPaymentMethodSelected: (PaymentMethod) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.values().take(2).forEach { method ->
                SelectableChip(
                    text = method.label,
                    selected = selectedPaymentMethod == method,
                    modifier = Modifier.weight(1f),
                    onClick = { onPaymentMethodSelected(method) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.values().drop(2).forEach { method ->
                SelectableChip(
                    text = method.label,
                    selected = selectedPaymentMethod == method,
                    modifier = Modifier.weight(1f),
                    onClick = { onPaymentMethodSelected(method) }
                )
            }
        }
    }
}

@Composable
internal fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val montserrat = montserratFamily()

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) FinTrackColors.GreenPrimary
                else Color(0xFFE1E7F0)
            )
            .border(
                width = 1.dp,
                color = if (selected) FinTrackColors.GreenPrimary else Color(0xFFC8D2E0),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF60748F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat
        )
    }
}

@Composable
internal fun DateField(
    value: String,
    onClick: () -> Unit,
    placeholder: String = "dd/mm/aaaa",
    /** US-17/US-18: el OCR no da un score de confianza por campo (solo detecta o no), así
     *  que se usa "sin detectar" (campo vacío) como equivalente práctico de "confianza baja"
     *  y se resalta con borde de advertencia para que el usuario lo revise/complete. */
    isMissing: Boolean = false
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Seleccionar fecha",
                    tint = colors.textSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(
                    if (isMissing) {
                        Modifier.border(1.5.dp, FinTrackColors.WarningColor, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colors.textPrimary
            ),
            singleLine = true,
            colors = formTextFieldColors()
        )

        // Superficie transparente encima del campo de solo lectura para capturar el tap
        // antes de que el TextField intente tomar foco de teclado.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
}

@Composable
private fun SaveTransactionButton(
    type: TransactionType,
    editing: Boolean,
    enabled: Boolean,
    isSaving: Boolean = false,
    onClick: () -> Unit
) {
    val text = when {
        editing -> "Guardar cambios"
        type == TransactionType.EXPENSE -> "Guardar gasto"
        else -> "Guardar ingreso"
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinTrackColors.GreenPrimary,
            disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.45f)
        )
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            return@Button
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserratFamily()
        )
    }
}

@Composable
internal fun formTextFieldColors(): TextFieldColors {
    val colors = LocalAppColors.current
    return TextFieldDefaults.colors(
        focusedContainerColor = colors.subtleSurface,
        unfocusedContainerColor = colors.subtleSurface,
        disabledContainerColor = colors.subtleSurface,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary
    )
}