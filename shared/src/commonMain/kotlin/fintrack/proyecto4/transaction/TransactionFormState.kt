package fintrack.proyecto4.transaction

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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

fun todayAsFormFieldDate(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val day = today.dayOfMonth.toString().padStart(2, '0')
    val month = today.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${today.year}"
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
                selectedCategory != null
}