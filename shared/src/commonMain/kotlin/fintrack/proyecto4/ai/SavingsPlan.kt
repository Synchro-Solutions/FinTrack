package fintrack.proyecto4.ai

import kotlinx.serialization.Serializable

@Serializable
data class SavingsPlan(
    /**
     * Monto recomendado que el usuario debería ahorrar mensualmente.
     */
    val monthlySaving: Double = 0.0,

    /**
     * Categorías de gastos que la IA recomienda revisar o reducir.
     */
    val categoriesToReduce: List<String> = emptyList(),

    /**
     * Explicación breve y personalizada del plan generado.
     */
    val explanation: String = ""
) {

    /**
     * Indica si el plan generado contiene información válida.
     */
    val isValid: Boolean
        get() = monthlySaving > 0.0 &&
                explanation.isNotBlank()
}