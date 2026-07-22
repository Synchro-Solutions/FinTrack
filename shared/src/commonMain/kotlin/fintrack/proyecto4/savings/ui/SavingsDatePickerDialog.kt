package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalTime::class
)
@Composable
fun SavingsDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val today = remember {
        Clock.System.now()
            .toLocalDateTime(
                TimeZone.currentSystemDefault()
            )
            .date
    }

    val todayMillis = remember(today) {
        today.toEpochDays().toLong() * MILLIS_PER_DAY
    }

    val selectableDates = remember(todayMillis) {
        object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                return utcTimeMillis >= todayMillis
            }

            override fun isSelectableYear(
                year: Int
            ): Boolean {
                return year >= today.year
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(
            containerColor =
                colors.surface
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis =
                        datePickerState.selectedDateMillis

                    when {
                        selectedMillis == null -> {
                            errorMessage =
                                "Selecciona una fecha."
                        }

                        selectedMillis < todayMillis -> {
                            errorMessage =
                                "La fecha no puede ser anterior a hoy."
                        }

                        else -> {
                            errorMessage = null

                            onDateSelected(
                                formatEpochMillisToDate(
                                    selectedMillis
                                )
                            )
                        }
                    }
                }
            ) {
                Text(
                    text = "Aceptar",
                    color = FinTrackColors.GreenPrimary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancelar",
                    color = FinTrackColors.GreenPrimary
                )
            }
        }
    ) {
        Column {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor =
                        colors.surface,

                    titleContentColor =
                        colors.textPrimary,

                    headlineContentColor =
                        colors.textPrimary,

                    weekdayContentColor =
                        colors.textSecondary,

                    subheadContentColor =
                        colors.textPrimary,

                    navigationContentColor =
                        colors.textPrimary,

                    yearContentColor =
                        colors.textPrimary,

                    currentYearContentColor =
                        FinTrackColors.GreenPrimary,

                    selectedYearContentColor =
                        FinTrackColors.White,

                    selectedYearContainerColor =
                        FinTrackColors.GreenPrimary,

                    dayContentColor =
                        colors.textPrimary,

                    disabledDayContentColor =
                        colors.textSecondary
                            .copy(alpha = 0.35f),

                    selectedDayContentColor =
                        FinTrackColors.White,

                    selectedDayContainerColor =
                        FinTrackColors.GreenPrimary,

                    todayContentColor =
                        FinTrackColors.GreenPrimary,

                    todayDateBorderColor =
                        FinTrackColors.GreenPrimary,

                    dividerColor =
                        colors.divider
                )
            )

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = FinTrackColors.ErrorColor,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 8.dp
                    )
                )
            }
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun formatEpochMillisToDate(
    millis: Long
): String {
    val date = LocalDate.fromEpochDays(
        (millis / MILLIS_PER_DAY).toInt()
    )

    val day = date.dayOfMonth
        .toString()
        .padStart(2, '0')

    val month = date.monthNumber
        .toString()
        .padStart(2, '0')

    return "$day/$month/${date.year}"
}