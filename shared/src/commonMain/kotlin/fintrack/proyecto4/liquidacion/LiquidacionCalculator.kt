package fintrack.proyecto4.liquidacion

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

private const val DAYS_PER_MONTH_REFERENCE = 30.0
private const val DIAS_TRES_MESES = 90
private const val DIAS_SEIS_MESES = 182
private const val DIAS_UN_ANIO = 365
private const val TOPE_ANIOS_CESANTIA = 8

/**
 * Días de auxilio de cesantía por año reconocido (Art. 29 Código de Trabajo de Costa Rica),
 * tope de 8 años (~167.74 días acumulados). Cifras verificadas cruzando dos fuentes
 * especializadas en derecho laboral costarricense (ver LiquidacionMessages.ABOUT_CALCULATOR).
 */
private val CESANTIA_DIAS_POR_ANIO = mapOf(
    1 to 19.5, 2 to 20.0, 3 to 20.5, 4 to 21.0,
    5 to 21.24, 6 to 21.5, 7 to 22.0, 8 to 22.0
)

/**
 * Calculadora pura de liquidación laboral (preaviso + cesantía) para Costa Rica. Sin dependencias
 * de Compose, testeable directamente. Vacaciones y aguinaldo proporcionales NO se incluyen aquí:
 * ya tienen su propia calculadora en el Centro Financiero (ver LiquidacionMessages.ABOUT_CALCULATOR).
 */
object LiquidacionCalculator {

    fun calculate(input: LiquidacionCalculationInput): LiquidacionCalculationOutcome {
        val errors = validate(input)
        if (errors.isNotEmpty()) return LiquidacionCalculationOutcome.Failure(errors)

        val ingreso = input.fechaIngreso!!
        val salida = input.fechaSalida!!
        val totalDias = ingreso.daysUntil(salida)
        val salarioDiario = input.salarioPromedioMensual / DAYS_PER_MONTH_REFERENCE

        val aplicaPreavisoYCesantia = input.motivoSalida == MotivoSalida.DESPIDO_SIN_JUSTA_CAUSA
        val aniosReconocidos = aniosReconocidosCesantia(ingreso, salida)

        val diasPreaviso = if (aplicaPreavisoYCesantia) diasPreaviso(totalDias) else 0.0
        val diasCesantia = if (aplicaPreavisoYCesantia) diasCesantia(totalDias, aniosReconocidos) else 0.0

        val montoPreaviso = diasPreaviso * salarioDiario
        val montoCesantia = diasCesantia * salarioDiario

        return LiquidacionCalculationOutcome.Success(
            LiquidacionCalculationResult(
                totalDiasTrabajados = totalDias,
                aniosReconocidosCesantia = aniosReconocidos,
                diasPreaviso = diasPreaviso,
                diasCesantia = diasCesantia,
                salarioDiario = salarioDiario,
                montoPreaviso = montoPreaviso,
                montoCesantia = montoCesantia,
                montoTotal = montoPreaviso + montoCesantia,
                aplicaPreaviso = aplicaPreavisoYCesantia,
                aplicaCesantia = aplicaPreavisoYCesantia
            )
        )
    }

    internal fun validate(input: LiquidacionCalculationInput): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        val ingreso = input.fechaIngreso
        val salida = input.fechaSalida

        if (ingreso == null) errors["fechaIngreso"] = "La fecha de ingreso es obligatoria."
        if (salida == null) errors["fechaSalida"] = "La fecha de salida es obligatoria."
        if (ingreso != null && salida != null && salida < ingreso) {
            errors["fechaSalida"] = "La fecha de salida debe ser igual o posterior a la fecha de ingreso."
        }
        if (!input.salarioPromedioMensual.isFinite()) {
            errors["salarioPromedioMensual"] = "El salario promedio no es válido."
        } else if (input.salarioPromedioMensual < 0.0) {
            errors["salarioPromedioMensual"] = "El salario promedio no puede ser negativo."
        }

        return errors
    }

    /** Art. 28: 1 semana (3-6 meses), 15 días (6 meses-1 año), 1 mes (más de 1 año). */
    private fun diasPreaviso(totalDias: Int): Double = when {
        totalDias < DIAS_TRES_MESES -> 0.0
        totalDias < DIAS_SEIS_MESES -> 7.0
        totalDias < DIAS_UN_ANIO -> 15.0
        else -> 30.0
    }

    /**
     * Art. 29: para menos de 1 año usa los tramos fijos (7/14 días); desde el año 1 suma el
     * factor de [CESANTIA_DIAS_POR_ANIO] de cada año reconocido (tope 8 años).
     */
    private fun diasCesantia(totalDias: Int, aniosReconocidos: Int): Double = when {
        totalDias < DIAS_TRES_MESES -> 0.0
        totalDias < DIAS_SEIS_MESES -> 7.0
        totalDias < DIAS_UN_ANIO -> 14.0
        else -> (1..aniosReconocidos).sumOf { anio -> CESANTIA_DIAS_POR_ANIO[anio] ?: 22.0 }
    }

    /**
     * Años completos entre ambas fechas; si el tramo final supera los 6 meses cuenta como un
     * año adicional (regla de fracción del Art. 29), con tope de [TOPE_ANIOS_CESANTIA].
     */
    private fun aniosReconocidosCesantia(ingreso: LocalDate, salida: LocalDate): Int {
        var years = 0
        var cursor = ingreso
        while (true) {
            val next = cursor.plusYearsSafe(1)
            if (next > salida) break
            years++
            cursor = next
        }
        val remainderDays = cursor.daysUntil(salida)
        if (remainderDays > DIAS_SEIS_MESES) years++

        return years.coerceAtMost(TOPE_ANIOS_CESANTIA)
    }

    /** Suma años a una fecha; si cae en 29 de febrero y el año destino no es bisiesto, usa 28. */
    private fun LocalDate.plusYearsSafe(years: Int): LocalDate {
        return try {
            LocalDate(year + years, monthNumber, day)
        } catch (e: IllegalArgumentException) {
            LocalDate(year + years, monthNumber, day - 1)
        }
    }
}
