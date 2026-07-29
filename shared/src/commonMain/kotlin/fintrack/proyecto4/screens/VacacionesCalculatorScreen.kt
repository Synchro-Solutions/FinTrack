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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.screens.common.AppDatePickerDialog
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColones
import fintrack.proyecto4.vacation.VacationCalculationInput
import fintrack.proyecto4.vacation.VacationCalculationOutcome
import fintrack.proyecto4.vacation.VacationCalculationResult
import fintrack.proyecto4.vacation.VacationCalculator
import fintrack.proyecto4.vacation.VacationMessages
import fintrack.proyecto4.vacation.VacationPaymentModality
import fintrack.proyecto4.vacation.formatVacationDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private enum class DateFieldTarget { START, CUTOFF }

@Composable
fun VacacionesCalculatorScreen(onBack: () -> Unit = {}) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current

    var employmentStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var cutoffDate by remember { mutableStateOf<LocalDate?>(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var paymentModality by remember { mutableStateOf(VacationPaymentModality.MONTHLY_OR_BIWEEKLY) }
    var salaryText by remember { mutableStateOf("") }
    var datePickerTarget by remember { mutableStateOf<DateFieldTarget?>(null) }

    val grossMonthlySalary = salaryText.toDoubleOrNull() ?: 0.0
    val outcome = if (employmentStartDate != null && cutoffDate != null) {
        VacationCalculator.calculate(
            VacationCalculationInput(
                employmentStartDate = employmentStartDate,
                cutoffDate = cutoffDate,
                paymentModality = paymentModality,
                grossMonthlySalary = grossMonthlySalary
            )
        )
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        ScreenHeader(title = "Calculadora de Vacaciones", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = VacationMessages.SUBTITLE,
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
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(FinTrackColors.GreenPrimary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = VacationMessages.LEGAL_BASIS,
                        color = colors.textSecondary,
                        fontFamily = montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            }

            SectionCard(title = "Fechas") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VacationDateField(
                        label = "Fecha de ingreso",
                        value = employmentStartDate,
                        onValueChange = { employmentStartDate = it },
                        onOpenCalendar = { datePickerTarget = DateFieldTarget.START },
                        montserrat = montserrat,
                        modifier = Modifier.weight(1f)
                    )
                    VacationDateField(
                        label = "Fecha de corte",
                        value = cutoffDate,
                        onValueChange = { cutoffDate = it },
                        onOpenCalendar = { datePickerTarget = DateFieldTarget.CUTOFF },
                        montserrat = montserrat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionCard(title = "Modalidad de pago") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val options = listOf(
                        Triple(VacationPaymentModality.MONTHLY_OR_BIWEEKLY, "Mensual / Quincenal", "14 días por período (incluye descanso semanal)"),
                        Triple(VacationPaymentModality.WEEKLY_NON_COMMERCE, "Semanal (no comercio)", "12 días por período (solo días laborados)"),
                        Triple(VacationPaymentModality.WEEKLY_COMMERCE, "Semanal (comercio)", "14 días por período (descanso es pago obligatorio)"),
                        Triple(VacationPaymentModality.DOMESTIC_SERVICE, "Servicio doméstico", "15 días por período (1.25 días por mes)")
                    )
                    options.forEachIndexed { index, (modality, title, description) ->
                        ModalityOption(
                            title = title,
                            description = description,
                            selected = paymentModality == modality,
                            onClick = { paymentModality = modality },
                            montserrat = montserrat
                        )
                        if (index != options.lastIndex) {
                            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            SectionCard(title = "Salario") {
                Text(
                    text = "Salario bruto mensual",
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
                    text = VacationMessages.SALARY_HELPER,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp
                )
            }

            when (outcome) {
                is VacationCalculationOutcome.Success -> ResultSection(result = outcome.result, montserrat = montserrat)
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = VacationMessages.EMPTY_RESULT_PLACEHOLDER,
                        color = colors.textSecondary,
                        fontFamily = montserrat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            SectionCard(title = VacationMessages.ABOUT_CALCULATOR_TITLE) {
                Text(
                    text = VacationMessages.ABOUT_CALCULATOR,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = VacationMessages.ABOUT_CALCULATOR_LAST_UPDATED,
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
            DateFieldTarget.START -> employmentStartDate
            DateFieldTarget.CUTOFF -> cutoffDate
        }
        AppDatePickerDialog(
            initialDate = initial,
            onDismiss = { datePickerTarget = null },
            onDateConfirmed = { picked ->
                when (target) {
                    DateFieldTarget.START -> employmentStartDate = picked
                    DateFieldTarget.CUTOFF -> cutoffDate = picked
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
            .background(colors.surface)
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
private fun ModalityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    montserrat: FontFamily
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FinTrackColors.GreenPrimary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
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
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
            Text(
                text = description,
                color = colors.textSecondary,
                fontFamily = montserrat,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VacationDateField(
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
                visualTransformation = SalaryThousandsVisualTransformation,
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

private object SalaryThousandsVisualTransformation : VisualTransformation {
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
private fun ResultSection(result: VacationCalculationResult, montserrat: FontFamily) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultTile(
                label = "Días totales",
                value = formatVacationDays(result.totalDays),
                valueColor = FinTrackColors.GreenPrimary,
                modifier = Modifier.weight(1f)
            )
            ResultTile(
                label = "Monto estimado",
                value = formatColones(kotlin.math.round(result.amountToPay).toLong()),
                valueColor = FinTrackColors.GreenPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard {
            BreakdownRow("Días por período", formatVacationDays(result.daysPerPeriod), montserrat)
            BreakdownRow("Períodos completos", result.completedPeriods.toString(), montserrat)
            BreakdownRow("Días consolidados", formatVacationDays(result.consolidatedDays), montserrat)
            BreakdownRow("Días proporcionales", formatVacationDays(result.proportionalDays), montserrat)
            BreakdownRow("Salario diario", formatColones(kotlin.math.round(result.dailyWage).toLong()), montserrat)
        }

        if (result.isProportionalOnly) {
            Text(
                text = VacationMessages.PROPORTIONAL_NOTICE,
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
    valueColor: androidx.compose.ui.graphics.Color? = null
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
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

private fun formatLocalDateUs(date: LocalDate): String {
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
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
