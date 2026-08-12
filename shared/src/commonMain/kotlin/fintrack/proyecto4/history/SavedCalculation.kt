package fintrack.proyecto4.history

/** Tipo de calculadora que originó el cálculo guardado. El Conversor de moneda queda fuera a propósito. */
enum class CalculationType(val label: String) {
    AGUINALDO("Aguinaldo"),
    SALARIO_NETO("Salario neto"),
    VACACIONES("Vacaciones"),
    LIQUIDACION("Liquidación")
}

/**
 * Cálculo guardado por el usuario desde alguna calculadora del Centro Financiero. [detalle] es
 * un mapa libre (label -> valor ya formateado) para no crear un modelo distinto por cada tipo de
 * calculadora — la colección financialCalculations en Firestore no valida forma, así que esta
 * flexibilidad es segura.
 */
data class SavedCalculation(
    val id: String = "",
    val tipo: CalculationType,
    val fechaCalculo: Long,
    val resumen: String,
    val montoPrincipal: Long?,
    val detalle: Map<String, String> = emptyMap()
)
