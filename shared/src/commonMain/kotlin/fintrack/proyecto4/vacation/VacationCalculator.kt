package fintrack.proyecto4.vacation

import kotlinx.datetime.daysUntil
import kotlin.math.round

private const val WEEKS_PER_PERIOD = 50
private const val DAYS_PER_MONTH_REFERENCE = 30.0
private const val DOMESTIC_SERVICE_MONTHLY_RATE = 1.25

/**
 * Calculadora pura de vacaciones para Costa Rica, alineada con la calculadora del Ministerio de
 * Trabajo y Seguridad Social. Sin dependencias de Compose, testeable directamente. Los valores
 * internos se mantienen en Double sin redondear; el redondeo solo ocurre en [formatVacationDays],
 * usado por la capa de presentación.
 */
object VacationCalculator {

    fun calculate(input: VacationCalculationInput): VacationCalculationOutcome {
        val errors = validate(input)
        if (errors.isNotEmpty()) return VacationCalculationOutcome.Failure(errors)

        val start = input.employmentStartDate!!
        val cutoff = input.cutoffDate!!
        val totalDaysWorked = start.daysUntil(cutoff)
        val totalWeeks = totalDaysWorked / 7
        val completedPeriods = totalWeeks / WEEKS_PER_PERIOD
        val remainderWeeks = totalWeeks % WEEKS_PER_PERIOD

        val daysPerPeriod = daysPerPeriod(input.paymentModality)
        val consolidatedDays = completedPeriods * daysPerPeriod

        val proportionalDays = if (input.paymentModality == VacationPaymentModality.DOMESTIC_SERVICE) {
            val remainderDays = totalDaysWorked - completedPeriods * WEEKS_PER_PERIOD * 7
            (remainderDays / DAYS_PER_MONTH_REFERENCE) * DOMESTIC_SERVICE_MONTHLY_RATE
        } else {
            (remainderWeeks.toDouble() / WEEKS_PER_PERIOD) * daysPerPeriod
        }

        val totalDays = consolidatedDays + proportionalDays
        val dailyWage = input.grossMonthlySalary / DAYS_PER_MONTH_REFERENCE
        val amountToPay = totalDays * dailyWage

        return VacationCalculationOutcome.Success(
            VacationCalculationResult(
                totalWeeksWorked = totalWeeks,
                completedPeriods = completedPeriods,
                daysPerPeriod = daysPerPeriod,
                consolidatedDays = consolidatedDays,
                proportionalDays = proportionalDays,
                totalDays = totalDays,
                dailyWage = dailyWage,
                amountToPay = amountToPay,
                isProportionalOnly = completedPeriods == 0
            )
        )
    }

    internal fun validate(input: VacationCalculationInput): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        val start = input.employmentStartDate
        val cutoff = input.cutoffDate

        if (start == null) {
            errors["employmentStartDate"] = "La fecha de ingreso es obligatoria."
        }
        if (cutoff == null) {
            errors["cutoffDate"] = "La fecha de corte es obligatoria."
        }
        if (start != null && cutoff != null && cutoff < start) {
            errors["cutoffDate"] = "La fecha de corte debe ser igual o posterior a la fecha de ingreso."
        }

        if (!input.grossMonthlySalary.isFinite()) {
            errors["grossMonthlySalary"] = "El salario bruto mensual no es válido."
        } else if (input.grossMonthlySalary < 0.0) {
            errors["grossMonthlySalary"] = "El salario bruto mensual no puede ser negativo."
        }

        return errors
    }

    private fun daysPerPeriod(modality: VacationPaymentModality): Double = when (modality) {
        VacationPaymentModality.MONTHLY_OR_BIWEEKLY -> 14.0
        VacationPaymentModality.WEEKLY_NON_COMMERCE -> 12.0
        VacationPaymentModality.WEEKLY_COMMERCE -> 14.0
        VacationPaymentModality.DOMESTIC_SERVICE -> 15.0
    }
}

/**
 * Formatea una cantidad de días para presentación, redondeando únicamente aquí (máximo dos
 * decimales) y sin ocultar decimales relevantes. Ejemplos: 8.0 -> "8 días", 8.5 -> "8.5 días",
 * 8.333333 -> "8.33 días".
 */
fun formatVacationDays(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    val text = if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        val hundredths = round(rounded * 100.0).toLong()
        val wholePart = hundredths / 100
        val fractionPart = kotlin.math.abs(hundredths % 100)
        if (fractionPart % 10 == 0L) {
            "$wholePart.${fractionPart / 10}"
        } else {
            "$wholePart.${fractionPart.toString().padStart(2, '0')}"
        }
    }
    return "$text días"
}
