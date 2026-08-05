package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.screens.common.SuccessSnackbarHost
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.FinTrackTypography
import fintrack.proyecto4.theme.LightAppColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.theme.subtleSurface
import fintrack.proyecto4.transaction.CustomCategory
import fintrack.proyecto4.transaction.CustomCategoryRepository
import fintrack.proyecto4.transaction.MaxDescriptionLength
import fintrack.proyecto4.transaction.NoOpCustomCategoryRepository
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

/** Rojo para Gasto, verde para Ingreso — mismo acento que ya usan TransactionsScreen/
 *  TransactionDetailScreen/DashboardScreen para distinguir movimientos por tipo. */
@Composable
private fun typeAccentColor(type: TransactionType): Color =
    if (type == TransactionType.EXPENSE) FinTrackColors.ErrorColor else FinTrackColors.GreenPrimary

/** Cuánto se muestra el Snackbar de éxito antes de navegar fuera de la pantalla. */
private const val SuccessSnackbarDelayMillis = 900L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    initialType: TransactionType = TransactionType.EXPENSE,
    editingTransaction: Transaction? = null,
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    categoryRepository: CustomCategoryRepository = NoOpCustomCategoryRepository(),
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onOcrClick: () -> Unit = {},
    cameraContent: @Composable (onCaptured: (String) -> Unit, onCancel: () -> Unit) -> Unit =
        { _, onCancel -> onCancel() },
    onPickReceiptImage: (onPicked: (String?) -> Unit) -> Unit = { onPicked -> onPicked(null) },
    uploadReceiptPhoto: suspend (String) -> Result<String> = {
        Result.failure(UnsupportedOperationException("Subida de comprobantes no configurada"))
    }
) {
    val uid = AuthClient.currentUserId() ?: ""
    // remember (no viewModel(key=...)) a propósito: cada visita a esta pantalla debe partir
    // de un formulario en blanco (o precargado solo con la transacción a editar). Con
    // viewModel(key=...) el ViewModelStore de la Activity reutilizaba la misma instancia
    // entre visitas consecutivas de "nueva transacción" (misma key), dejando los campos de
    // la transacción anterior visibles al crear una nueva.
    val viewModel = remember(uid, editingTransaction?.id, initialType) {
        TransactionFormViewModel(
            repository = transactionRepository,
            uid = uid,
            initialType = initialType,
            editingTransaction = editingTransaction,
            categoryRepository = categoryRepository,
            uploadReceipt = uploadReceiptPhoto
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val categoryFormError by viewModel.categoryFormError.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showReceiptCamera by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }
    var categoryPendingDelete by remember { mutableStateOf<CustomCategory?>(null) }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val categoriesForType = customCategories.filter { it.type == state.type }

    val accentColor = typeAccentColor(state.type)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TransactionHeader(
            title = if (viewModel.isEditing) "Editar movimiento" else "Nuevo movimiento",
            subtitle = if (viewModel.isEditing) {
                "Edita los datos de tu movimiento"
            } else {
                "Registra un ingreso o gasto"
            },
            // El asistente OCR solo tiene sentido al crear una transacción nueva a partir de
            // un comprobante; al editar una ya existente no hay nada que escanear.
            showOcrButton = !viewModel.isEditing,
            onBack = onBack,
            onOcrClick = onOcrClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TransactionTypeTabs(
                selectedType = state.type,
                // US-14: el tipo (Ingreso/Gasto) no se puede cambiar tras guardar la
                // transacción, así que en modo edición el selector queda bloqueado.
                locked = viewModel.isEditing,
                onTypeSelected = viewModel::changeType
            )

            FormCard {
                FormSectionTitle("Monto")
                Spacer(Modifier.height(10.dp))
                AmountField(
                    value = state.amount,
                    accentColor = accentColor,
                    onValueChange = viewModel::updateAmount
                )
            }

            FormCard {
                FormSectionTitle("Descripción")
                Spacer(Modifier.height(10.dp))
                DescriptionField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription
                )

                Spacer(Modifier.height(16.dp))

                FieldLabel("Fecha")
                DateField(
                    value = state.date,
                    onClick = { showDatePicker = true }
                )
            }

            FormCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormSectionTitle("Categoría")
                    if (categoriesForType.isNotEmpty()) {
                        Text(
                            text = "Gestionar categorías",
                            color = FinTrackColors.GreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = montserratFamily(),
                            modifier = Modifier.clickable { showManageCategories = true }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                CategorySection(
                    categories = state.categories,
                    customCategories = categoriesForType,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::selectCategory,
                    onAddCategory = {
                        editingCategory = null
                        viewModel.clearCategoryFormError()
                        showCategoryDialog = true
                    }
                )
            }

            FormCard {
                FormSectionTitle("Método de pago")
                Spacer(Modifier.height(10.dp))
                PaymentMethodSection(
                    selectedPaymentMethod = state.paymentMethod,
                    onPaymentMethodSelected = viewModel::selectPaymentMethod
                )
            }

            FormCard {
                FormSectionTitle("Comprobante")
                Spacer(Modifier.height(10.dp))
                ReceiptSection(
                    receiptUrl = state.receiptUrl,
                    isUploading = state.isUploadingReceipt,
                    onTakePhotoClick = { showReceiptCamera = true },
                    onPickImageClick = {
                        onPickReceiptImage { path -> if (path != null) viewModel.onReceiptPicked(path) }
                    }
                )
            }

            if (saveError != null) {
                Text(
                    text = saveError ?: "",
                    color = FinTrackColors.ErrorColor,
                    fontSize = 12.sp,
                    fontFamily = montserratFamily()
                )
            }

            SaveTransactionButton(
                type = state.type,
                editing = viewModel.isEditing,
                enabled = state.isValid && !isSaving && !state.isUploadingReceipt,
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

    if (showReceiptCamera) {
        cameraContent(
            { path -> showReceiptCamera = false; viewModel.onReceiptPicked(path) },
            { showReceiptCamera = false }
        )
    }
    }

    if (showDatePicker) {
        FintrackDatePickerDialog(
            initialDateMillis = parseDateToEpochMillis(state.date),
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { millis -> viewModel.updateDate(formatEpochMillisToDate(millis)) }
        )
    }

    if (showCategoryDialog) {
        CategoryFormDialog(
            existing = editingCategory,
            errorMessage = categoryFormError,
            onDismiss = {
                showCategoryDialog = false
                viewModel.clearCategoryFormError()
            },
            onConfirm = { name, icon ->
                val current = editingCategory
                val onSuccess = { showCategoryDialog = false }
                if (current == null) {
                    viewModel.createCategory(name, icon, onSuccess = onSuccess)
                } else {
                    viewModel.updateCategory(current, name, icon, onSuccess = onSuccess)
                }
            }
        )
    }

    if (showManageCategories) {
        ManageCategoriesSheet(
            categories = categoriesForType,
            onDismiss = { showManageCategories = false },
            onEdit = { category ->
                editingCategory = category
                viewModel.clearCategoryFormError()
                showManageCategories = false
                showCategoryDialog = true
            },
            onDeleteRequest = { category ->
                categoryPendingDelete = category
            }
        )
    }

    categoryPendingDelete?.let { category ->
        DeleteCategoryConfirmDialog(
            category = category,
            onDismiss = { categoryPendingDelete = null },
            onConfirm = {
                viewModel.deleteCategory(category)
                categoryPendingDelete = null
            }
        )
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

/**
 * Esquema de color claro para el `MaterialTheme` que envuelve el DatePicker. [DatePickerColors]
 * (usado en [fintrackDatePickerColors]) no expone un parámetro para el color del campo de
 * ingreso manual de fecha (el ícono de lápiz/teclado dentro del picker) — ese campo hereda el
 * `MaterialTheme.colorScheme` ambiental. Sin este override, en modo oscuro el texto tecleado
 * salía gris casi ilegible sobre la tarjeta blanca forzada del picker.
 */
private val LightDatePickerColorScheme = lightColorScheme(
    primary = LightAppColors.primary,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = LightAppColors.textPrimary,
    onSurfaceVariant = LightAppColors.textSecondary,
    outline = LightAppColors.border,
    outlineVariant = LightAppColors.divider
)

/**
 * DatePicker único de la app (formulario de transacción, confirmación OCR, filtros del
 * historial): siempre tarjeta blanca sin importar el tema activo, con `MaterialTheme` claro
 * forzado (ver [LightDatePickerColorScheme]) para que también el modo de ingreso manual de
 * fecha se lea bien.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FintrackDatePickerDialog(
    initialDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        selectableDates = NotFutureSelectableDates
    )

    MaterialTheme(colorScheme = LightDatePickerColorScheme, typography = FinTrackTypography()) {
        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            colors = fintrackDatePickerColors(),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FinTrackColors.GreenPrimary)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(contentColor = LightAppColors.textSecondary)
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = fintrackDatePickerColors())
        }
    }
}

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
private fun TransactionHeader(
    title: String,
    subtitle: String,
    showOcrButton: Boolean = true,
    onBack: () -> Unit,
    onOcrClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = colors.textPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack)
        )

        Spacer(Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = montserrat
            )
        }

        if (showOcrButton) {
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
            .height(52.dp)
            .alpha(if (locked) 0.6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TransactionTypeTab(
            text = "Gasto",
            selected = selectedType == TransactionType.EXPENSE,
            accentColor = FinTrackColors.ErrorColor,
            enabled = !locked,
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected(TransactionType.EXPENSE) }
        )

        TransactionTypeTab(
            text = "Ingreso",
            selected = selectedType == TransactionType.INCOME,
            accentColor = FinTrackColors.GreenPrimary,
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
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accentColor else colors.subtleSurface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat
        )
    }
}

/** Título de sección dentro de una tarjeta del formulario (p. ej. "Monto", "Detalles"). */
@Composable
private fun FormSectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.textPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = montserratFamily()
    )
}

/** Etiqueta de un campo puntual dentro de una tarjeta (p. ej. "Fecha"). */
@Composable
private fun FieldLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = montserratFamily()
    )
}

/** Tarjeta que agrupa una sección del formulario, mismo estilo que el resto de la app
 *  (superficie + esquinas redondeadas), en vez de campos sueltos sobre el fondo. */
@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .glassCard()
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun AmountField(
    value: String,
    accentColor: Color,
    onValueChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₡",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily()
                )
            }
        },
        placeholder = {
            Text(
                text = "0",
                color = colors.textSecondary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
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
        label = {
            Text(
                text = "Descripción (máx. $MaxDescriptionLength caracteres)",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = montserratFamily()
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            color = colors.textPrimary,
            fontFamily = montserratFamily()
        ),
        singleLine = true,
        colors = formTextFieldColors()
    )
    Text(
        text = "${value.length}/$MaxDescriptionLength",
        color = colors.textSecondary,
        fontSize = 11.sp,
        fontFamily = montserratFamily(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, end = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}

@Composable
internal fun CategorySection(
    categories: List<String>,
    customCategories: List<CustomCategory>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
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
        customCategories.forEach { category ->
            SelectableChip(
                text = category.name,
                icon = category.icon,
                selected = selectedCategory == category.name,
                onClick = { onCategorySelected(category.name) }
            )
        }
        AddCategoryChip(onClick = onAddCategory)
    }
}

/** Chip con borde punteado-estilo (outline verde, sin relleno) para diferenciarlo de las
 *  opciones seleccionables — no es una categoría, es la acción de crear una nueva. */
@Composable
internal fun AddCategoryChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = FinTrackColors.GreenPrimary,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+ ",
                color = FinTrackColors.GreenPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
            Text(
                text = "Nueva categoría",
                color = FinTrackColors.GreenPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
        }
    }
}

@Composable
internal fun PaymentMethodSection(
    selectedPaymentMethod: PaymentMethod?,
    onPaymentMethodSelected: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun ReceiptSection(
    receiptUrl: String?,
    isUploading: Boolean,
    onTakePhotoClick: () -> Unit,
    onPickImageClick: () -> Unit
) {
    val colors = LocalAppColors.current

    when {
        isUploading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = FinTrackColors.GreenPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Subiendo comprobante...",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = montserratFamily()
            )
        }

        receiptUrl != null -> Column {
            AsyncImage(
                model = receiptUrl,
                contentDescription = "Comprobante adjunto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(12.dp))
            ReceiptPickerButtons(
                takePhotoLabel = "Reemplazar",
                onTakePhotoClick = onTakePhotoClick,
                onPickImageClick = onPickImageClick
            )
        }

        else -> ReceiptPickerButtons(
            takePhotoLabel = "Tomar foto",
            onTakePhotoClick = onTakePhotoClick,
            onPickImageClick = onPickImageClick
        )
    }
}

@Composable
private fun ReceiptPickerButtons(
    takePhotoLabel: String,
    onTakePhotoClick: () -> Unit,
    onPickImageClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onTakePhotoClick,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = takePhotoLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
        }

        OutlinedButton(
            onClick = onPickImageClick,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Elegir de galería",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
        }
    }
}

@Composable
internal fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: String? = null,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                color = if (selected) Color.White else Color(0xFF60748F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
        }
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
                        Modifier.border(1.5.dp, FinTrackColors.ErrorColor, RoundedCornerShape(16.dp))
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

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .defaultMinSize(minWidth = 200.dp)
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinTrackColors.GreenDark,
            disabledContainerColor = FinTrackColors.GreenDark.copy(alpha = 0.45f)
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

// ── US-06: categorías personalizadas ────────────────────────────────────────

/** Emojis sugeridos para categorías, mismo criterio de "íconos predefinidos" que pedía la
 *  historia (US-06: "Lista de íconos predefinidos"), sin forzar un catálogo cerrado — el
 *  usuario igual puede escribir cualquier otro emoji a mano. */
private val SuggestedCategoryIcons = listOf(
    "🛒", "🍔", "🚌", "🏠", "💡", "🎮",
    "👕", "📚", "💊", "✈️", "🎁", "💰",
    "📱", "🐾", "⚽", "🎵", "☕", "🔧"
)

/** Único diálogo para crear o editar una categoría personalizada — [existing] null significa
 *  modo creación. Sin color a propósito (fuera de alcance); el ícono es un emoji opcional,
 *  elegible desde una grilla de sugerencias o escrito a mano (máx. 2 caracteres). */
@Composable
internal fun CategoryFormDialog(
    existing: CustomCategory?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String?) -> Unit
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FinTrackColors.GreenPrimary,
        unfocusedBorderColor = colors.border,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = FinTrackColors.GreenPrimary,
        unfocusedLabelColor = colors.textSecondary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (existing == null) "Nueva categoría" else "Editar categoría",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Vista previa: refleja en vivo el ícono elegido (o las iniciales del
                // nombre si todavía no hay ícono) sobre un círculo verde, mismo lenguaje
                // visual que el avatar del onboarding.
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FinTrackColors.GreenPrimary.copy(alpha = 0.15f))
                            .border(1.5.dp, FinTrackColors.GreenPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon.ifBlank { name.take(2).uppercase().ifBlank { "🏷️" } },
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinTrackColors.GreenPrimary
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Mascotas", color = colors.textSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel("ÍCONO (OPCIONAL)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SuggestedCategoryIcons.forEach { suggestion ->
                            val selected = icon == suggestion
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) FinTrackColors.GreenPrimary
                                        else colors.subtleSurface
                                    )
                                    .border(
                                        width = if (selected) 0.dp else 1.dp,
                                        color = colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { icon = if (selected) "" else suggestion },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = suggestion, fontSize = 18.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { if (it.length <= 2) icon = it },
                        label = { Text("Otro emoji") },
                        placeholder = { Text("Ej: 🎯", color = colors.textSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = FinTrackColors.ErrorColor,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, icon) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = colors.surfaceSecondary,
                    disabledContentColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (existing == null) "Crear" else "Guardar",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        }
    )
}

/** Panel para editar/eliminar las categorías personalizadas del tipo actual (Gasto/Ingreso).
 *  Las predefinidas no aparecen aquí porque no tienen affordance de edición — ya se seleccionan
 *  directo desde los chips de CategorySection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageCategoriesSheet(
    categories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onEdit: (CustomCategory) -> Unit,
    onDeleteRequest: (CustomCategory) -> Unit
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gestionar categorías",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily()
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "Aún no tienes categorías personalizadas. Créalas desde el chip \"+ Nueva categoría\".",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.subtleSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (category.icon != null) {
                                Text(text = category.icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(
                                text = category.name,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = montserratFamily(),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onEdit(category) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar categoría",
                                    tint = colors.textSecondary
                                )
                            }
                            IconButton(onClick = { onDeleteRequest(category) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar categoría",
                                    tint = FinTrackColors.ErrorColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Confirmación de borrado de una categoría personalizada — ícono de advertencia, vista
 *  previa de la categoría a borrar y botón "Eliminar" relleno en rojo (acción destructiva,
 *  mismo peso visual que el botón principal verde de CategoryFormDialog). */
@Composable
internal fun DeleteCategoryConfirmDialog(
    category: CustomCategory,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(FinTrackColors.ErrorColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = FinTrackColors.ErrorColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Eliminar categoría",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.subtleSurface)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (category.icon != null) {
                        Text(text = category.icon, fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(
                        text = category.name,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = montserratFamily()
                    )
                }
                Text(
                    text = "Esta acción no se puede deshacer. Las transacciones que ya usaban esta " +
                        "categoría se reasignarán automáticamente a \"Otro\".",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = montserratFamily(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.ErrorColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Eliminar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = colors.textSecondary)
            }
        }
    )
}