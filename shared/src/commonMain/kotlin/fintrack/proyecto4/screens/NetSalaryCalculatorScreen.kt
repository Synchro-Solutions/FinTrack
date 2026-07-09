package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import kotlin.math.round

/** Rebaja adicional opcional agregada manualmente por el usuario (ej. pensión, préstamo). */
private data class Deduction(val name: String, val amount: Long)

/** Cuota obrera de la CCSS en Costa Rica (SEM + IVM), editable en "Editar parámetros". */
private const val DEFAULT_CCSS_RATE_PERCENT = 10.67

private data class TaxBracket(val upperLimit: Long?, val rate: Double)

/** Tramos mensuales de referencia del impuesto de renta en Costa Rica (aproximados, se actualizan cada año). */
private val incomeTaxBrackets = listOf(
    TaxBracket(922_000L, 0.0),
    TaxBracket(1_352_000L, 0.10),
    TaxBracket(2_373_000L, 0.15),
    TaxBracket(4_745_000L, 0.20),
    TaxBracket(null, 0.25)
)

/** Calcula el impuesto de renta mensual estimado aplicando los tramos progresivos de [incomeTaxBrackets]. */
private fun calculateEstimatedIncomeTax(grossSalary: Long): Long {
    if (grossSalary <= 0) return 0L
    var tax = 0.0
    var lowerBound = 0L
    for (bracket in incomeTaxBrackets) {
        val upperBound = bracket.upperLimit ?: grossSalary
        if (grossSalary > lowerBound) {
            val taxableInBracket = minOf(grossSalary, upperBound) - lowerBound
            tax += taxableInBracket * bracket.rate
        }
        lowerBound = upperBound
        if (bracket.upperLimit != null && grossSalary <= bracket.upperLimit) break
    }
    return round(tax).toLong()
}

private fun calculateCcssContribution(grossSalary: Long, ratePercent: Double): Long =
    round(grossSalary * ratePercent / 100.0).toLong()

/** Formatea un monto en colones con separador de miles '.', ej. 850000 -> "₡850.000", -50000 -> "-₡50.000". */
private fun formatColonesDot(amount: Long): String {
    val isNegative = amount < 0
    val abs = if (isNegative) -amount else amount
    val grouped = abs.toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (isNegative) "-" else ""}₡$grouped"
}

private fun sanitizeDigitsInput(input: String): String = input.filter(Char::isDigit)

/** Deja pasar solo dígitos y un único separador decimal, usado para el porcentaje de CCSS. */
private fun sanitizePercentInput(input: String): String {
    val result = StringBuilder()
    var hasDecimalPoint = false
    for (char in input) {
        when {
            char.isDigit() -> result.append(char)
            (char == '.' || char == ',') && !hasDecimalPoint -> {
                result.append('.')
                hasDecimalPoint = true
            }
        }
    }
    return result.toString()
}

private object NetSalaryThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = formatThousandsWithDots(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                return formatThousandsWithDots(raw.take(safe)).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, formatted.length)
                return formatted.take(safe).count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

private fun formatThousandsWithDots(digits: String): String {
    if (digits.isEmpty()) return ""
    return digits.reversed().chunked(3).joinToString(".").reversed()
}

@Composable
fun NetSalaryCalculatorScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current

    var grossSalaryText by remember { mutableStateOf("") }
    var ccssRatePercent by remember { mutableStateOf(DEFAULT_CCSS_RATE_PERCENT) }
    var autoIncomeTaxEnabled by remember { mutableStateOf(true) }
    val deductions = remember { mutableListOf<Deduction>().toMutableStateList() }

    var showAddDeductionPanel by remember { mutableStateOf(false) }
    var showEditParams by remember { mutableStateOf(false) }

    val grossSalary = grossSalaryText.toLongOrNull() ?: 0L
    val grossSalaryIsInvalid = grossSalaryText.isNotBlank() && grossSalary <= 0L
    val ccssAmount = calculateCcssContribution(grossSalary, ccssRatePercent)
    val incomeTax = if (autoIncomeTaxEnabled) calculateEstimatedIncomeTax(grossSalary) else 0L
    val otherDeductionsTotal = deductions.sumOf { it.amount }
    val netSalary = grossSalary - ccssAmount - incomeTax - otherDeductionsTotal
    val hasNetSalaryWarning = grossSalary > 0 && netSalary <= 0

    fun resetCalculator() {
        grossSalaryText = ""
        ccssRatePercent = DEFAULT_CCSS_RATE_PERCENT
        autoIncomeTaxEnabled = true
        deductions.clear()
        showAddDeductionPanel = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        ScreenHeader(
            title = "Salario neto estimado",
            onBack = onBack,
            trailingContent = {
                IconButton(onClick = { resetCalculator() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar cálculo",
                        tint = colors.textPrimary
                    )
                }
            }
        )
        Text(
            text = "Estimación mensual para Costa Rica",
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(text = "SALARIO BRUTO MENSUAL", montserrat = montserrat)
                GrossSalaryField(
                    value = grossSalaryText,
                    onValueChange = { grossSalaryText = sanitizeDigitsInput(it) },
                    montserrat = montserrat,
                    height = 64.dp,
                    fontSize = 28.sp
                )
                if (grossSalaryIsInvalid) {
                    FieldErrorText(text = "Ingrese el salario bruto", montserrat = montserrat)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BreakdownRow(label = "Salario bruto", amount = grossSalary, montserrat = montserrat)
                    BreakdownRow(label = "CCSS trabajador", amount = -ccssAmount, montserrat = montserrat)
                    BreakdownRow(
                        label = if (autoIncomeTaxEnabled) "Renta estimada" else "Renta estimada (desactivada)",
                        amount = -incomeTax,
                        montserrat = montserrat
                    )

                    if (deductions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "OTRAS REBAJAS",
                            color = colors.textPrimary,
                            fontFamily = montserrat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        deductions.forEachIndexed { index, deduction ->
                            DeductionRow(
                                deduction = deduction,
                                montserrat = montserrat,
                                onRemove = { deductions.removeAt(index) }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    if (showAddDeductionPanel) {
                        AddDeductionPanel(
                            grossSalary = grossSalary,
                            montserrat = montserrat,
                            onCancel = { showAddDeductionPanel = false },
                            onConfirm = { name, amount ->
                                deductions.add(Deduction(name, amount))
                                showAddDeductionPanel = false
                            }
                        )
                    } else {
                        TextButton(
                            onClick = { showAddDeductionPanel = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = FinTrackColors.BlueMeta,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Agregar rebaja",
                                color = FinTrackColors.BlueMeta,
                                fontFamily = montserrat,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    NetSalaryBanner(
                        netSalary = netSalary,
                        hasWarning = hasNetSalaryWarning,
                        montserrat = montserrat
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parámetros configurables. Resultado estimado.",
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showEditParams = true }) {
                    Text(
                        text = "Editar parámetros",
                        color = colors.textPrimary,
                        fontFamily = montserrat,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = { onSaved() },
                enabled = grossSalary > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
            ) {
                Text(
                    text = "Guardar cálculo",
                    fontFamily = montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showEditParams) {
        EditParamsDialog(
            ccssRatePercent = ccssRatePercent,
            autoIncomeTaxEnabled = autoIncomeTaxEnabled,
            onDismiss = { showEditParams = false },
            onConfirm = { newRate, newAutoTax ->
                ccssRatePercent = newRate
                autoIncomeTaxEnabled = newAutoTax
                showEditParams = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String, montserrat: FontFamily) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.textPrimary,
        fontFamily = montserrat,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun FieldErrorText(text: String, montserrat: FontFamily) {
    Text(
        text = text,
        color = FinTrackColors.ErrorColor,
        fontFamily = montserrat,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun GrossSalaryField(
    value: String,
    onValueChange: (String) -> Unit,
    montserrat: FontFamily,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceSecondary)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₡",
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = NetSalaryThousandsVisualTransformation,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            color = FinTrackColors.WhiteAlpha40,
                            fontFamily = montserrat,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Long, montserrat: FontFamily) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = formatColonesDot(amount),
            color = if (amount < 0) FinTrackColors.ErrorColor else colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Fila individual de una rebaja agregada manualmente, con acción de eliminar. */
@Composable
private fun DeductionRow(deduction: Deduction, montserrat: FontFamily, onRemove: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = deduction.name,
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatColonesDot(-deduction.amount),
            color = FinTrackColors.ErrorColor,
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Eliminar rebaja",
                tint = colors.textPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun NetSalaryBanner(
    netSalary: Long,
    hasWarning: Boolean,
    montserrat: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hasWarning) FinTrackColors.GradientRed else FinTrackColors.GradientGreen)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Salario neto estimado",
            color = FinTrackColors.White.copy(alpha = 0.85f),
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = formatColonesDot(netSalary),
            color = FinTrackColors.White,
            fontFamily = montserrat,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        if (hasWarning) {
            Text(
                text = "El salario neto quedó en cero o es negativo.",
                color = FinTrackColors.White.copy(alpha = 0.9f),
                fontFamily = montserrat,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Mini-menú inline (no modal) para agregar una rebaja: se expande justo donde estaba el
 * botón "+ Agregar rebaja", con chips de nombres frecuentes, campo de nombre y monto.
 */
@Composable
private fun AddDeductionPanel(
    grossSalary: Long,
    montserrat: FontFamily,
    onCancel: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val amount = amountText.toLongOrNull() ?: 0L
    val amountExceedsGross = grossSalary > 0 && amount > grossSalary
    val canConfirm = nameText.isNotBlank() && amount > 0 && !amountExceedsGross

    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceSecondary)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InlineTextField(
            value = nameText,
            onValueChange = { nameText = it },
            placeholder = "Nombre de la rebaja",
            montserrat = montserrat,
            keyboardType = KeyboardType.Text
        )

        InlineTextField(
            value = amountText,
            onValueChange = { amountText = sanitizeDigitsInput(it) },
            placeholder = "Monto",
            montserrat = montserrat,
            keyboardType = KeyboardType.Number,
            visualTransformation = NetSalaryThousandsVisualTransformation
        )

        if (amountExceedsGross) {
            FieldErrorText(text = "La rebaja no puede superar el salario bruto", montserrat = montserrat)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Cancelar", fontFamily = montserrat, color = colors.textPrimary)
            }
            Button(
                onClick = { onConfirm(nameText.trim(), amount) },
                enabled = canConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.BlueMeta)
            ) {
                Text(text = "Agregar", fontFamily = montserrat, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    montserrat: FontFamily,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = FinTrackColors.WhiteAlpha40,
                        fontFamily = montserrat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun EditParamsDialog(
    ccssRatePercent: Double,
    autoIncomeTaxEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ccssRatePercent: Double, autoIncomeTaxEnabled: Boolean) -> Unit
) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current
    var rateText by remember { mutableStateOf(formatRateForInput(ccssRatePercent)) }
    var autoTax by remember { mutableStateOf(autoIncomeTaxEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(text = "Editar parámetros", fontFamily = montserrat, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Porcentaje CCSS trabajador",
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                InlineTextField(
                    value = rateText,
                    onValueChange = { rateText = sanitizePercentInput(it) },
                    placeholder = "Porcentaje",
                    montserrat = montserrat,
                    keyboardType = KeyboardType.Decimal
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calcular renta automáticamente",
                        color = colors.textPrimary,
                        fontFamily = montserrat,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = autoTax,
                        onCheckedChange = { autoTax = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FinTrackColors.GreenPrimary,
                            checkedTrackColor = FinTrackColors.GreenPrimary.copy(alpha = 0.5f),
                            uncheckedThumbColor = colors.textPrimary,
                            uncheckedTrackColor = colors.surfaceSecondary
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedRate = rateText.toDoubleOrNull()?.takeIf { it > 0 } ?: ccssRatePercent
                    onConfirm(parsedRate, autoTax)
                }
            ) {
                Text(text = "Guardar", fontFamily = montserrat, color = FinTrackColors.GreenPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", fontFamily = montserrat, color = colors.textPrimary)
            }
        }
    )
}

private fun formatRateForInput(rate: Double): String =
    if (rate == rate.toLong().toDouble()) rate.toLong().toString() else rate.toString()
