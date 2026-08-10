package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.liquidacion.LiquidacionCalculationInput
import fintrack.proyecto4.liquidacion.LiquidacionCalculationOutcome
import fintrack.proyecto4.liquidacion.LiquidacionCalculationResult
import fintrack.proyecto4.liquidacion.LiquidacionCalculator
import fintrack.proyecto4.liquidacion.LiquidacionMessages
import fintrack.proyecto4.liquidacion.MotivoSalida
import fintrack.proyecto4.screens.common.AppDatePickerDialog
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColones
import kotlinx.datetime.LocalDate

private enum class LiquidacionDateFieldTarget { INGRESO, SALIDA }

@Composable
fun LiquidacionCalculatorScreen(onBack: () -> Unit = {}) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current

    var fechaIngreso by remember { mutableStateOf<LocalDate?>(null) }
    var fechaSalida by remember { mutableStateOf<LocalDate?>(null) }
    var motivoSalida by remember { mutableStateOf(MotivoSalida.DESPIDO_SIN_JUSTA_CAUSA) }
    var salaryText by remember { mutableStateOf("") }
    var datePickerTarget by remember { mutableStateOf<LiquidacionDateFieldTarget?>(null) }

    val salarioPromedioMensual = salaryText.toDoubleOrNull() ?: 0.0
    val outcome = LiquidacionCalculator.calculate(
        LiquidacionCalculationInput(
            fechaIngreso = fechaIngreso,
            fechaSalida = fechaSalida,
            salarioPromedioMensual = salarioPromedioMensual,
            motivoSalida = motivoSalida
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Calculadora de Liquidación", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = LiquidacionMessages.SUBTITLE,
                color = colors.textSecondary,
                fontFamily = montserrat,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            SectionCard {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(FinTrackColors.GreenPrimary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = LiquidacionMessages.LEGAL_BASIS,
                        color = colors.textSecondary,
                        fontFamily = montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            }

            SectionCard(title = "Motivo de salida") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MotivoSalida.entries.forEachIndexed { index, motivo ->
                        MotivoOption(
                            title = motivo.label,
                            selected = motivoSalida == motivo,
                            onClick = { motivoSalida = motivo },
                            montserrat = montserrat
                        )
                    }
                }
            }

            SectionCard(title = "Fechas") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidacionDateField(
                        label = "Fecha de ingreso",
                        value = fechaIngreso,
                        onValueChange = { fechaIngreso = it },
                        onOpenCalendar = { datePickerTarget = LiquidacionDateFieldTarget.INGRESO },
                        montserrat = montserrat,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidacionDateField(
                        label = "Fecha de salida",
                        value = fechaSalida,
                        onValueChange = { fechaSalida = it },
                        onOpenCalendar = { datePickerTarget = LiquidacionDateFieldTarget.SALIDA },
                        montserrat = montserrat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionCard(title = "Salario") {
                Text(
                    text = "Salario promedio mensual (últimos 6 meses)",
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                SalaryField(
                    value = salaryText,
                    onValueChange = { salaryText = it.filter(Char::isDigit) },
                    montserrat = montserrat
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = LiquidacionMessages.SALARY_HELPER,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp
                )
            }

            when (outcome) {
                is LiquidacionCalculationOutcome.Success -> ResultSection(
                    result = outcome.result,
                    motivoSalida = motivoSalida,
                    montserrat = montserrat
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .glassCard()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LiquidacionMessages.EMPTY_RESULT_PLACEHOLDER,
                        color = colors.textSecondary,
                        fontFamily = montserrat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            SectionCard(title = LiquidacionMessages.ABOUT_CALCULATOR_TITLE) {
                Text(
                    text = LiquidacionMessages.ABOUT_CALCULATOR,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = LiquidacionMessages.ABOUT_CALCULATOR_LAST_UPDATED,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    datePickerTarget?.let { target ->
        val initial = when (target) {
            LiquidacionDateFieldTarget.INGRESO -> fechaIngreso
            LiquidacionDateFieldTarget.SALIDA -> fechaSalida
        }
        AppDatePickerDialog(
            initialDate = initial,
            onDismiss = { datePickerTarget = null },
            onDateConfirmed = { picked ->
                when (target) {
                    LiquidacionDateFieldTarget.INGRESO -> fechaIngreso = picked
                    LiquidacionDateFieldTarget.SALIDA -> fechaSalida = picked
                }
                datePickerTarget = null
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
        }
        content()
    }
}

@Composable
private fun MotivoOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    montserrat: FontFamily
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FinTrackColors.GreenPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = FinTrackColors.GreenPrimary, unselectedColor = colors.textSecondary),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun LiquidacionDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    onOpenCalendar: () -> Unit,
    montserrat: FontFamily,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var text by remember(value) { mutableStateOf(value?.let { formatLocalDateUs(it) } ?: "") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = colors.textPrimary, fontFamily = montserrat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSecondary)
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    val formatted = formatUsDateDigits(input)
                    text = formatted
                    onValueChange(parseUsInputDate(formatted))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "mm/dd/yyyy",
                            color = colors.textSecondary,
                            fontFamily = montserrat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            )
            IconButton(onClick = onOpenCalendar, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Abrir calendario",
                    tint = FinTrackColors.GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SalaryField(
    value: String,
    onValueChange: (String) -> Unit,
    montserrat: FontFamily
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSecondary),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₡",
                color = FinTrackColors.GreenPrimary,
                fontFamily = montserrat,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = LiquidacionThousandsVisualTransformation,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            color = colors.textSecondary,
                            fontFamily = montserrat,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

private object LiquidacionThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = raw.reversed().chunked(3).joinToString(".").reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                return raw.take(safe).reversed().chunked(3).joinToString(".").reversed().length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, formatted.length)
                return formatted.take(safe).count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
private fun ResultSection(result: LiquidacionCalculationResult, motivoSalida: MotivoSalida, montserrat: FontFamily) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ResultTile(
            label = "Total estimado de liquidación",
            value = formatColones(kotlin.math.round(result.montoTotal).toLong()),
            valueColor = FinTrackColors.GreenPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultTile(
                label = "Preaviso",
                value = formatColones(kotlin.math.round(result.montoPreaviso).toLong()),
                modifier = Modifier.weight(1f)
            )
            ResultTile(
                label = "Cesantía",
                value = formatColones(kotlin.math.round(result.montoCesantia).toLong()),
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard {
            BreakdownRow("Días trabajados", result.totalDiasTrabajados.toString(), montserrat)
            BreakdownRow("Años reconocidos (cesantía)", "${result.aniosReconocidosCesantia} de 8", montserrat)
            BreakdownRow("Días de preaviso", formatDays(result.diasPreaviso), montserrat)
            BreakdownRow("Días de cesantía", formatDays(result.diasCesantia), montserrat)
            BreakdownRow("Salario diario", formatColones(kotlin.math.round(result.salarioDiario).toLong()), montserrat)
        }

        val notice = when (motivoSalida) {
            MotivoSalida.RENUNCIA -> LiquidacionMessages.RENUNCIA_NOTICE
            MotivoSalida.DESPIDO_CON_JUSTA_CAUSA -> LiquidacionMessages.DESPIDO_JUSTA_CAUSA_NOTICE
            MotivoSalida.DESPIDO_SIN_JUSTA_CAUSA -> null
        }
        if (notice != null) {
            Text(
                text = notice,
                color = colors.textSecondary,
                fontFamily = montserrat,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun ResultTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .glassCard()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontFamily = montserrat,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor ?: colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, montserrat: FontFamily) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontFamily = montserrat,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Formatea días con hasta 2 decimales, sin ceros de relleno (ej. 19.5, 22, 21.24). */
private fun formatDays(value: Double): String {
    val hundredths = kotlin.math.round(value * 100.0).toLong()
    val wholePart = hundredths / 100
    val fractionPart = kotlin.math.abs(hundredths % 100)
    return if (fractionPart == 0L) {
        wholePart.toString()
    } else if (fractionPart % 10 == 0L) {
        "$wholePart.${fractionPart / 10}"
    } else {
        "$wholePart.${fractionPart.toString().padStart(2, '0')}"
    }
}

private fun formatLocalDateUs(date: LocalDate): String {
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')
    return "$month/$day/${date.year}"
}

/** Inserta las barras de mm/dd/yyyy automáticamente a medida que la persona escribe dígitos. */
private fun formatUsDateDigits(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    val builder = StringBuilder()
    digits.forEachIndexed { index, c ->
        if (index == 2 || index == 4) builder.append('/')
        builder.append(c)
    }
    return builder.toString()
}

private fun parseUsInputDate(text: String): LocalDate? {
    val digits = text.filter(Char::isDigit)
    if (digits.length != 8) return null
    val month = digits.substring(0, 2).toIntOrNull() ?: return null
    val day = digits.substring(2, 4).toIntOrNull() ?: return null
    val year = digits.substring(4, 8).toIntOrNull() ?: return null
    return try {
        LocalDate(year, month, day)
    } catch (e: IllegalArgumentException) {
        null
    }
}
