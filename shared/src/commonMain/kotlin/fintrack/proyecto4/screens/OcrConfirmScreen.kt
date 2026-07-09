package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
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
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.ocr.OcrResult
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.screens.common.SuccessSnackbarHost
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.theme.subtleSurface
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionFormViewModel
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cuánto se muestra el Snackbar de éxito antes de navegar fuera de la pantalla. */
private const val SuccessSnackbarDelayMillis = 900L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrConfirmScreen(
    result: OcrResult,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    transactionRepository: TransactionRepository = NoOpTransactionRepository()
) {
    val uid = AuthClient.currentUserId() ?: ""
    // remember (no viewModel(key=...)) a propósito: cada confirmación OCR debe partir de un
    // formulario limpio, no reutilizar categoría/método de pago de una confirmación anterior.
    val viewModel = remember(uid) {
        TransactionFormViewModel(transactionRepository, uid, TransactionType.EXPENSE)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(result) {
        viewModel.prefillFromOcr(result)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        ScreenHeader(title = "Confirmar transacción OCR", onBack = onCancel)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 24.dp)
        ) {
            WarningBanner()

            Spacer(Modifier.height(20.dp))

            OcrFieldLabel("MONTO (₡)")
            TextField(
                value = state.amount,
                onValueChange = viewModel::updateAmount,
                placeholder = {
                    Text(
                        text = "Dato no detectado",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(top = 6.dp)
                    .then(missingFieldBorder(isMissing = state.amount.isBlank())),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = formTextFieldColors()
            )

            Spacer(Modifier.height(18.dp))

            OcrFieldLabel("FECHA")
            DateField(
                value = state.date,
                onClick = { showDatePicker = true },
                placeholder = "Dato no detectado",
                isMissing = state.date.isBlank()
            )

            Spacer(Modifier.height(18.dp))

            OcrFieldLabel("COMERCIO")
            TextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                placeholder = {
                    Text(
                        text = "Dato no detectado",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = montserratFamily()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = 6.dp)
                    .then(missingFieldBorder(isMissing = state.description.isBlank())),
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    fontFamily = montserratFamily()
                ),
                singleLine = true,
                colors = formTextFieldColors()
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "CATEGORÍA",
                color = Color(0xFF58708F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    SelectableChip(
                        text = category,
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "MÉTODO DE PAGO",
                color = Color(0xFF58708F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
            PaymentMethodSection(
                selectedPaymentMethod = state.paymentMethod,
                onPaymentMethodSelected = viewModel::selectPaymentMethod
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

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveTransaction {
                            scope.launch {
                                launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Movimiento registrado exitosamente",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                delay(SuccessSnackbarDelayMillis)
                                onSaved()
                            }
                        }
                    },
                    enabled = state.isValid && state.paymentMethod != null && !isSaving,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackColors.GreenPrimary,
                        disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.45f)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirmar y guardar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = montserratFamily()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = colors.textSecondary
                    )
                }
            }
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
        FintrackDatePickerDialog(
            initialDateMillis = parseDateToEpochMillis(state.date),
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { millis -> viewModel.updateDate(formatEpochMillisToDate(millis)) }
        )
    }
}

@Composable
private fun WarningBanner() {
    val colors = LocalAppColors.current
    val bannerBg = if (colors.isDark) FinTrackColors.WarningColor.copy(alpha = 0.12f) else colors.subtleSurface
    val bannerBorder = if (colors.isDark) FinTrackColors.WarningColor.copy(alpha = 0.35f) else colors.border
    val bannerTextColor = if (colors.isDark) FinTrackColors.WarningLight else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bannerBg)
            .border(1.dp, bannerBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = FinTrackColors.WarningColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Revisa los datos detectados. No se guardan automáticamente sin tu confirmación.",
            color = bannerTextColor,
            fontSize = 12.sp,
            fontFamily = montserratFamily()
        )
    }
}

/**
 * US-17/US-18: OcrResult no trae un score de confianza por campo (solo detecta o no),
 * así que se usa "campo vacío tras el prefill" como equivalente práctico de "confianza
 * baja" y se resalta con borde de advertencia (rojo, igual en ambos temas) para que el
 * usuario lo revise antes de confirmar. El borde desaparece en cuanto el usuario completa
 * el campo.
 */
private fun missingFieldBorder(isMissing: Boolean): Modifier =
    if (isMissing) {
        Modifier.border(1.5.dp, FinTrackColors.ErrorColor, RoundedCornerShape(16.dp))
    } else {
        Modifier
    }

@Composable
private fun OcrFieldLabel(text: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = text,
            color = Color(0xFF58708F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.subtleSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "· detectado por OCR",
                color = FinTrackColors.GreenPrimary,
                fontSize = 10.sp,
                fontFamily = montserratFamily()
            )
        }
    }
}
