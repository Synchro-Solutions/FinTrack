package fintrack.proyecto4.vacation

import kotlinx.datetime.LocalDate

/**
 * Modalidad de pago/jornada tal como la presenta la calculadora de vacaciones del Ministerio de
 * Trabajo y Seguridad Social de Costa Rica. Determina los dias por periodo y la forma de calcular
 * el proporcional cuando no se han completado las cincuenta semanas continuas.
 */
enum class VacationPaymentModality {
    MONTHLY_OR_BIWEEKLY,
    WEEKLY_NON_COMMERCE,
    WEEKLY_COMMERCE,
    DOMESTIC_SERVICE
}

data class VacationCalculationInput(
    val employmentStartDate: LocalDate?,
    val cutoffDate: LocalDate?,
    val paymentModality: VacationPaymentModality,
    val grossMonthlySalary: Double
)

data class VacationCalculationResult(
    val totalWeeksWorked: Int,
    val completedPeriods: Int,
    val daysPerPeriod: Double,
    val consolidatedDays: Double,
    val proportionalDays: Double,
    val totalDays: Double,
    val dailyWage: Double,
    val amountToPay: Double,
    val isProportionalOnly: Boolean
)

/** Resultado del calculo: exito con [VacationCalculationResult] o fallo con errores por campo. */
sealed interface VacationCalculationOutcome {
    data class Success(val result: VacationCalculationResult) : VacationCalculationOutcome
    data class Failure(val fieldErrors: Map<String, String>) : VacationCalculationOutcome
}
