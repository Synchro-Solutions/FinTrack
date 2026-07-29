package fintrack.proyecto4.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private val MONTHS_FULL = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
private val MONTHS_SHORT = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
)
private val WEEKDAYS_SHORT = listOf("L", "M", "M", "J", "V", "S", "D")

private enum class PickerMode { CALENDAR, TEXT_INPUT }

private data class YearMonth(val year: Int, val month: Int) {
    fun plusMonths(delta: Int): YearMonth {
        val zeroBased = (year * 12 + (month - 1)) + delta
        return YearMonth(zeroBased.floorDiv(12), zeroBased.mod(12) + 1)
    }
}

/**
 * Selector de fecha reutilizable con la misma apariencia en toda la app (calendario propio,
 * en español, semana iniciando en lunes, día seleccionado en circulo verde). Se construye con
 * primitivas de Compose en vez del DatePicker de Material 3 para controlar exactamente el
 * layout pedido (encabezado con fecha grande, alternancia calendario/texto, selector de año).
 * Los colores se toman de [LocalAppColors] y [FinTrackColors] para respetar el tema actual.
 */
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateConfirmed: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val startDate = initialDate ?: today

    var selectedDate by rememberSaveable { mutableStateOf(startDate) }
    var displayedYearMonth by rememberSaveable { mutableStateOf(YearMonth(startDate.year, startDate.monthNumber)) }
    var mode by rememberSaveable { mutableStateOf(PickerMode.CALENDAR) }
    var showYearPicker by rememberSaveable { mutableStateOf(false) }
    var textInput by rememberSaveable { mutableStateOf(formatForInput(selectedDate)) }
    var textInputError by remember { mutableStateOf(false) }

    fun isOutOfRange(date: LocalDate): Boolean =
        (minDate != null && date < minDate) || (maxDate != null && date > maxDate)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Encabezado
            Text(
                text = "Seleccionar fecha",
                color = colors.textSecondary,
                fontFamily = montserrat,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatHeaderDate(selectedDate),
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    mode = if (mode == PickerMode.CALENDAR) PickerMode.TEXT_INPUT else PickerMode.CALENDAR
                    showYearPicker = false
                    textInput = formatForInput(selectedDate)
                    textInputError = false
                }) {
                    Icon(
                        imageVector = if (mode == PickerMode.CALENDAR) Icons.Default.Edit else Icons.Default.CalendarMonth,
                        contentDescription = if (mode == PickerMode.CALENDAR) "Ingresar fecha manualmente" else "Usar el calendario",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(14.dp))

            if (mode == PickerMode.TEXT_INPUT) {
                ManualDateInput(
                    value = textInput,
                    isError = textInputError,
                    montserrat = montserrat,
                    onValueChange = { digits ->
                        textInput = digits
                        parseInputDate(digits)?.let { parsed ->
                            if (!isOutOfRange(parsed)) {
                                selectedDate = parsed
                                displayedYearMonth = YearMonth(parsed.year, parsed.monthNumber)
                                textInputError = false
                            }
                        }
                    }
                )
            } else {
                // Navegación de mes/año
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showYearPicker = !showYearPicker },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${MONTHS_FULL[displayedYearMonth.month - 1].replaceFirstChar { it.uppercase() }} de ${displayedYearMonth.year}",
                            color = colors.textPrimary,
                            fontFamily = montserrat,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar mes y año",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!showYearPicker) {
                        Row {
                            IconButton(onClick = { displayedYearMonth = displayedYearMonth.plusMonths(-1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { displayedYearMonth = displayedYearMonth.plusMonths(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = colors.textPrimary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (showYearPicker) {
                    YearPicker(
                        selectedYear = displayedYearMonth.year,
                        montserrat = montserrat,
                        onYearSelected = { year ->
                            displayedYearMonth = YearMonth(year, displayedYearMonth.month)
                            showYearPicker = false
                        }
                    )
                } else {
                    CalendarGrid(
                        displayedYearMonth = displayedYearMonth,
                        selectedDate = selectedDate,
                        isOutOfRange = ::isOutOfRange,
                        montserrat = montserrat,
                        onDaySelected = { selectedDate = it }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = FinTrackColors.GreenPrimary, fontFamily = montserrat, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    if (mode == PickerMode.TEXT_INPUT) {
                        val parsed = parseInputDate(textInput)
                        if (parsed == null || isOutOfRange(parsed)) {
                            textInputError = true
                            return@TextButton
                        }
                        onDateConfirmed(parsed)
                    } else {
                        onDateConfirmed(selectedDate)
                    }
                }) {
                    Text("Aceptar", color = FinTrackColors.GreenPrimary, fontFamily = montserrat, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    isOutOfRange: (LocalDate) -> Boolean,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    onDaySelected: (LocalDate) -> Unit
) {
    val colors = LocalAppColors.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        WEEKDAYS_SHORT.forEach { label ->
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    val firstOfMonth = LocalDate(displayedYearMonth.year, displayedYearMonth.month, 1)
    val firstOfNextMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    val daysInMonth = firstOfMonth.daysUntil(firstOfNextMonth)
    val leadingBlanks = firstOfMonth.dayOfWeek.ordinal

    val cells = buildList<LocalDate?> {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..daysInMonth) add(LocalDate(displayedYearMonth.year, displayedYearMonth.month, day))
        while (size % 7 != 0) add(null)
    }
    val weeks = cells.chunked(7)

    Column {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        selected = date != null && date == selectedDate,
                        disabled = date != null && isOutOfRange(date),
                        montserrat = montserrat,
                        onClick = { date?.let(onDaySelected) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    disabled: Boolean,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (date != null && !disabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date == null) return@Box
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (selected) FinTrackColors.GreenPrimary else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    selected -> FinTrackColors.White
                    disabled -> colors.textSecondary.copy(alpha = 0.4f)
                    else -> colors.textPrimary
                },
                fontFamily = montserrat,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun YearPicker(
    selectedYear: Int,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    onYearSelected: (Int) -> Unit
) {
    val colors = LocalAppColors.current
    val years = remember { (1950..selectedYear + 50).toList() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(240.dp)
    ) {
        items(years) { year ->
            val isSelected = year == selectedYear
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) FinTrackColors.GreenPrimary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onYearSelected(year) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = year.toString(),
                    color = if (isSelected) FinTrackColors.White else colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ManualDateInput(
    value: String,
    isError: Boolean,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    onValueChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input -> onValueChange(formatDateInputDigits(input)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontFamily = montserrat,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "dd/mm/aaaa",
                            color = colors.textSecondary,
                            fontFamily = montserrat,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
        if (isError) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ingresa una fecha válida (dd/mm/aaaa) dentro del rango permitido.",
                color = FinTrackColors.ErrorColor,
                fontFamily = montserrat,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDateInputDigits(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    val builder = StringBuilder()
    digits.forEachIndexed { index, c ->
        if (index == 2 || index == 4) builder.append('/')
        builder.append(c)
    }
    return builder.toString()
}

private fun parseInputDate(text: String): LocalDate? {
    val digits = text.filter(Char::isDigit)
    if (digits.length != 8) return null
    val day = digits.substring(0, 2).toIntOrNull() ?: return null
    val month = digits.substring(2, 4).toIntOrNull() ?: return null
    val year = digits.substring(4, 8).toIntOrNull() ?: return null
    return try {
        LocalDate(year, month, day)
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun formatForInput(date: LocalDate): String {
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

private fun formatHeaderDate(date: LocalDate): String {
    val month = MONTHS_SHORT[date.monthNumber - 1]
    return "${date.dayOfMonth} $month ${date.year}"
}
