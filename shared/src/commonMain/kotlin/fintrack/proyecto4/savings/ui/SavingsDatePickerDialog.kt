package fintrack.proyecto4.savings.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun SavingsDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tomorrowMillis = ((Clock.System.now().epochSeconds / 86_400) + 1) * 86_400_000L

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = tomorrowMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis

                    if (selectedMillis == null || selectedMillis < tomorrowMillis) {
                        errorMessage = "Seleccione una fecha futura"
                    } else {
                        onDateSelected(formatEpochMillisToDate(selectedMillis))
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatEpochMillisToDate(millis: Long): String {
    val date = LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}