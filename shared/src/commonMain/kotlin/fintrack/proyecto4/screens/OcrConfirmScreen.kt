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
import fintrack.proyecto4.ocr.OcrResult
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.TransactionFormViewModel
import fintrack.proyecto4.transaction.TransactionType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrConfirmScreen(
    result: OcrResult,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel = remember { TransactionFormViewModel(TransactionType.EXPENSE) }
    val state by viewModel.uiState.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(result) {
        viewModel.prefillFromOcr(result)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
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
                        color = FinTrackColors.TextSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = FinTrackColors.TextPrimary,
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
                placeholder = "Dato no detectado"
            )

            Spacer(Modifier.height(18.dp))

            OcrFieldLabel("COMERCIO")
            TextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                placeholder = {
                    Text(
                        text = "Dato no detectado",
                        color = FinTrackColors.TextSecondary,
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
                        viewModel.saveTransaction { result ->
                            if (result.isSuccess) onSaved()
                        }
                    },
                    enabled = state.isValid && state.paymentMethod != null,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackColors.GreenPrimary,
                        disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = "Confirmar y guardar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserratFamily()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, FinTrackColors.BorderDefault, RoundedCornerShape(14.dp))
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = FinTrackColors.TextSecondary
                    )
                }
            }
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
                    colors = ButtonDefaults.textButtonColors(contentColor = FinTrackColors.GreenPrimary)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = FinTrackColors.TextSecondary)
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = fintrackDatePickerColors())
        }
    }
}

@Composable
private fun WarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinTrackColors.WarningColor.copy(alpha = 0.12f))
            .border(1.dp, FinTrackColors.WarningColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
            color = FinTrackColors.WarningLight,
            fontSize = 12.sp,
            fontFamily = montserratFamily()
        )
    }
}

@Composable
private fun OcrFieldLabel(text: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = text,
            color = Color(0xFF58708F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "· detectado por OCR",
            color = FinTrackColors.GreenPrimary,
            fontSize = 10.sp,
            fontFamily = montserratFamily()
        )
    }
}
