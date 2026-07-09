package fintrack.proyecto4.transaction

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    CARD("Tarjeta"),
    SINPE("SINPE Móvil"),
    TRANSFER("Transferencia")
}

const val MaxDescriptionLength = 200

fun todayAsFormFieldDate(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val day = today.day.toString().padStart(2, '0')
    val month = today.month.number.toString().padStart(2, '0')
    return "$day/$month/${today.year}"
}

/** Parsea una fecha en formato "dd/mm/aaaa" (el único que usa el formulario). */
internal fun parseFormFieldDate(value: String): LocalDate? {
    val parts = value.split("/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return try {
        LocalDate(year, month, day)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/** Una fecha en blanco (dato no detectado por OCR) no cuenta como futura. */
fun isFutureFormFieldDate(value: String): Boolean {
    if (value.isBlank()) return false
    val date = parseFormFieldDate(value) ?: return false
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return date > today
}

data class TransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val description: String = "",
    val selectedCategory: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val date: String = todayAsFormFieldDate()
) {
    val categories: List<String>
        get() = when (type) {
            TransactionType.EXPENSE -> listOf(
                "Alimentación",
                "Transporte",
                "Vivienda",
                "Servicios",
                "Salud",
                "Entretenimiento",
                "Ropa",
                "Educación",
                "Otro"
            )

            TransactionType.INCOME -> listOf(
                "Salario",
                "Extra",
                "Inversión",
                "Regalo",
                "Otro"
            )
        }

    val isValid: Boolean
        get() = amount.isNotBlank() &&
                amount != "0" &&
                description.isNotBlank() &&
                description.length <= MaxDescriptionLength &&
                selectedCategory != null &&
                !isFutureFormFieldDate(date)
}