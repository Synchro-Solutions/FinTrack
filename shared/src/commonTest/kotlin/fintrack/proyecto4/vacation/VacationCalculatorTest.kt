package fintrack.proyecto4.vacation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VacationCalculatorTest {

    private fun success(input: VacationCalculationInput): VacationCalculationResult {
        val outcome = VacationCalculator.calculate(input)
        assertTrue(outcome is VacationCalculationOutcome.Success, "Se esperaba un resultado exitoso pero fue $outcome")
        return outcome.result
    }

    private fun failure(input: VacationCalculationInput): Map<String, String> {
        val outcome = VacationCalculator.calculate(input)
        assertTrue(outcome is VacationCalculationOutcome.Failure, "Se esperaba un error de validación pero fue $outcome")
        return outcome.fieldErrors
    }

    @Test
    fun `mensual quincenal con cincuenta semanas exactas consolida catorce dias`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(50 * 7, DateTimeUnit.DAY)
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = 600_000.0
            )
        )

        assertEquals(1, result.completedPeriods)
        assertEquals(14.0, result.daysPerPeriod)
        assertEquals(14.0, result.consolidatedDays)
        assertEquals(0.0, result.proportionalDays)
        assertEquals(14.0, result.totalDays)
        assertEquals(false, result.isProportionalOnly)
    }

    @Test
    fun `mensual quincenal antes de completar el periodo entrega proporcional`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(25 * 7, DateTimeUnit.DAY) // la mitad de las 50 semanas
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = 600_000.0
            )
        )

        assertEquals(0, result.completedPeriods)
        assertEquals(0.0, result.consolidatedDays)
        assertEquals(7.0, result.proportionalDays) // 25/50 * 14
        assertEquals(7.0, result.totalDays)
        assertTrue(result.isProportionalOnly)
    }

    @Test
    fun `semanal no comercio usa doce dias por periodo`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(50 * 7, DateTimeUnit.DAY)
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.WEEKLY_NON_COMMERCE,
                grossMonthlySalary = 0.0
            )
        )

        assertEquals(12.0, result.daysPerPeriod)
        assertEquals(12.0, result.consolidatedDays)
    }

    @Test
    fun `semanal comercio usa catorce dias por periodo`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(50 * 7, DateTimeUnit.DAY)
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.WEEKLY_COMMERCE,
                grossMonthlySalary = 0.0
            )
        )

        assertEquals(14.0, result.daysPerPeriod)
        assertEquals(14.0, result.consolidatedDays)
    }

    @Test
    fun `servicio domestico con periodo completo consolida quince dias`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(50 * 7, DateTimeUnit.DAY)
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.DOMESTIC_SERVICE,
                grossMonthlySalary = 0.0
            )
        )

        assertEquals(15.0, result.daysPerPeriod)
        assertEquals(15.0, result.consolidatedDays)
        assertEquals(0.0, result.proportionalDays)
    }

    @Test
    fun `servicio domestico proporcional usa uno coma veinticinco dias por mes`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(3, DateTimeUnit.MONTH) // tres meses, no llega a las 50 semanas
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.DOMESTIC_SERVICE,
                grossMonthlySalary = 0.0
            )
        )

        assertEquals(0.0, result.consolidatedDays)
        assertTrue(result.proportionalDays > 3.5 && result.proportionalDays < 3.9, "Se esperaban ~3.75 dias (3 meses x 1.25), fue ${result.proportionalDays}")
        assertTrue(result.isProportionalOnly)
    }

    @Test
    fun `el monto a pagar usa el salario diario sobre treinta dias`() {
        val start = LocalDate(2023, 1, 1)
        val cutoff = start.plus(50 * 7, DateTimeUnit.DAY)
        val result = success(
            VacationCalculationInput(
                employmentStartDate = start,
                cutoffDate = cutoff,
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = 300_000.0
            )
        )

        assertEquals(10_000.0, result.dailyWage) // 300000 / 30
        assertEquals(140_000.0, result.amountToPay) // 14 dias * 10000
    }

    @Test
    fun `fecha de ingreso obligatoria`() {
        val errors = failure(
            VacationCalculationInput(
                employmentStartDate = null,
                cutoffDate = LocalDate(2024, 1, 1),
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = 0.0
            )
        )
        assertTrue(errors.containsKey("employmentStartDate"))
    }

    @Test
    fun `fecha de corte anterior a la de ingreso produce error`() {
        val errors = failure(
            VacationCalculationInput(
                employmentStartDate = LocalDate(2024, 1, 1),
                cutoffDate = LocalDate(2023, 1, 1),
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = 0.0
            )
        )
        assertTrue(errors.containsKey("cutoffDate"))
    }

    @Test
    fun `salario negativo produce error`() {
        val errors = failure(
            VacationCalculationInput(
                employmentStartDate = LocalDate(2023, 1, 1),
                cutoffDate = LocalDate(2024, 1, 1),
                paymentModality = VacationPaymentModality.MONTHLY_OR_BIWEEKLY,
                grossMonthlySalary = -1.0
            )
        )
        assertTrue(errors.containsKey("grossMonthlySalary"))
    }

    @Test
    fun `formatVacationDays no oculta decimales relevantes`() {
        assertEquals("8 días", formatVacationDays(8.0))
        assertEquals("8.5 días", formatVacationDays(8.5))
        assertEquals("8.33 días", formatVacationDays(8.333333))
    }
}
