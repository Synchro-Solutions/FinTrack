package fintrack.proyecto4.util

import fintrack.proyecto4.dashboard.MonthlyChartData
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.transaction.parseFormFieldDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal fun Month.toSpanishLabel(): String {
    return when (this) {
        Month.JANUARY -> "Enero"
        Month.FEBRUARY -> "Febrero"
        Month.MARCH -> "Marzo"
        Month.APRIL -> "Abril"
        Month.MAY -> "Mayo"
        Month.JUNE -> "Junio"
        Month.JULY -> "Julio"
        Month.AUGUST -> "Agosto"
        Month.SEPTEMBER -> "Septiembre"
        Month.OCTOBER -> "Octubre"
        Month.NOVEMBER -> "Noviembre"
        Month.DECEMBER -> "Diciembre"
    }
}

/**
 * Agrega ingresos/gastos por mes para los últimos [monthsBack] meses (incluyendo el actual).
 * Usada tanto por el Dashboard (6 meses fijos) como por Reportes (rango variable por preset).
 */
internal fun buildMonthlyChartData(
    transactions: List<Transaction>,
    monthsBack: Int
): List<MonthlyChartData> {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    return (monthsBack - 1 downTo 0).map { offset ->
        var monthNum = today.monthNumber - offset
        var year = today.year
        if (monthNum <= 0) {
            monthNum += 12
            year--
        }

        val label = Month(monthNum).toSpanishLabel().take(3)

        val monthTransactions = transactions.filter { transaction ->
            val fecha = parseFormFieldDate(transaction.date) ?: return@filter false
            fecha.monthNumber == monthNum && fecha.year == year
        }

        MonthlyChartData(
            mes = label,
            ingresos = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount },
            gastos = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        )
    }
}
