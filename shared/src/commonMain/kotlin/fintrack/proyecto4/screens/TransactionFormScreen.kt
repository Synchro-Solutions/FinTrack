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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.PaymentMethod
import fintrack.proyecto4.transaction.TransactionFormViewModel
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    initialType: TransactionType = TransactionType.EXPENSE,
    editingTransactionId: String? = null,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onOcrClick: () -> Unit = {}
) {
    val viewModel = remember(initialType, editingTransactionId) {
        TransactionFormViewModel(initialType, editingTransactionId)
    }
    val state by viewModel.uiState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
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
                enabled = state.isValid,
                onClick = {
                    viewModel.saveTransaction { result ->
                        if (result.isSuccess) onSaved()
                    }
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToEpochMillis(state.date)
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
                        contentColor = FinTrackColors.TextSecondary
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

@Composable
internal fun fintrackDatePickerColors() = DatePickerDefaults.colors(
    containerColor = Color.White,
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

@Composable
private fun TransactionHeader(title: String, onBack: () -> Unit, onOcrClick: () -> Unit) {
    val montserrat = montserratFamily()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinTrackColors.SurfacePrimary)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            tint = FinTrackColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack)
        )

        Spacer(Modifier.width(18.dp))

        Text(
            text = title,
            color = FinTrackColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserrat,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Escanear con OCR",
            tint = FinTrackColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onOcrClick)
        )
    }
}

@Composable
private fun TransactionTypeTabs(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE1E7F0))
    ) {
        TransactionTypeTab(
            text = "Gasto",
            selected = selectedType == TransactionType.EXPENSE,
            selectedColor = Color(0xFFE53935),
            modifier = Modifier.weight(1f),
            onClick = { onTypeSelected(TransactionType.EXPENSE) }
        )

        TransactionTypeTab(
            text = "Ingreso",
            selected = selectedType == TransactionType.INCOME,
            selectedColor = FinTrackColors.GreenPrimary,
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
            .clickable(onClick = onClick),
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
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "0",
                color = FinTrackColors.TextSecondary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(top = 6.dp),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = FinTrackColors.TextPrimary,
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
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Ej. Supermercado, Salario...",
                color = Color(0xFF7C8FA8),
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
            color = FinTrackColors.TextPrimary,
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
    placeholder: String = "dd/mm/aaaa"
) {
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
                    color = FinTrackColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Seleccionar fecha",
                    tint = FinTrackColors.TextSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = FinTrackColors.TextPrimary
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
internal fun formTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FinTrackColors.SurfaceSecondary,
    unfocusedContainerColor = FinTrackColors.SurfaceSecondary,
    disabledContainerColor = FinTrackColors.SurfaceSecondary,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = FinTrackColors.GreenPrimary,
    focusedTextColor = FinTrackColors.TextPrimary,
    unfocusedTextColor = FinTrackColors.TextPrimary
)