package fintrack.proyecto4.liquidacion

import kotlinx.datetime.LocalDate

/**
 * Motivo por el que termina la relación laboral. Determina si aplica cesantía y preaviso: en
 * Costa Rica, solo el despido sin justa causa da derecho a ambos (Arts. 28, 29 y 81 del Código
 * de Trabajo).
 */
enum class MotivoSalida(val label: String) {
    DESPIDO_SIN_JUSTA_CAUSA("Despido sin justa causa"),
    RENUNCIA("Renuncia voluntaria"),
    DESPIDO_CON_JUSTA_CAUSA("Despido con justa causa")
}

data class LiquidacionCalculationInput(
    val fechaIngreso: LocalDate?,
    val fechaSalida: LocalDate?,
    val salarioPromedioMensual: Double,
    val motivoSalida: MotivoSalida
)

data class LiquidacionCalculationResult(
    val totalDiasTrabajados: Int,
    val aniosReconocidosCesantia: Int,
    val diasPreaviso: Double,
    val diasCesantia: Double,
    val salarioDiario: Double,
    val montoPreaviso: Double,
    val montoCesantia: Double,
    val montoTotal: Double,
    val aplicaPreaviso: Boolean,
    val aplicaCesantia: Boolean
)

/** Resultado del calculo: exito con [LiquidacionCalculationResult] o fallo con errores por campo. */
sealed interface LiquidacionCalculationOutcome {
    data class Success(val result: LiquidacionCalculationResult) : LiquidacionCalculationOutcome
    data class Failure(val fieldErrors: Map<String, String>) : LiquidacionCalculationOutcome
}
