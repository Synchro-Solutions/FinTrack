package fintrack.proyecto4.ai

object SavingsPrompt {

    fun buildPlanPrompt(
        goalName: String,
        targetAmount: Double,
        deadline: String?,
        requiredMonthlySaving: Double,
        totalIncome: Long,
        totalExpenses: Long,
        expensesByCategory: Map<String, Long>
    ): String {

        val balance = totalIncome - totalExpenses

        val deadlineText = deadline
            ?.takeIf { it.isNotBlank() }
            ?: "Sin fecha límite definida"

        val categorySummary = expensesByCategory
            .filterValues { it > 0L }
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .joinToString(separator = "\n") { entry ->
                "- ${entry.key}: ₡${entry.value}"
            }
            .ifBlank {
                "- No existen gastos registrados por categoría"
            }

        return """
            Analiza la situación financiera del usuario y genera un plan
            de ahorro realista para alcanzar una meta.

            DATOS DE LA META:
            - Nombre: $goalName
            - Monto objetivo: ₡$targetAmount
            - Fecha límite: $deadlineText
            - Ahorro mensual mínimo calculado por la aplicación: ₡$requiredMonthlySaving

            RESUMEN FINANCIERO:
            - Ingresos registrados: ₡$totalIncome
            - Gastos registrados: ₡$totalExpenses
            - Balance disponible: ₡$balance

            GASTOS POR CATEGORÍA:
            $categorySummary

            INSTRUCCIONES:
            1. El monto monthlySaving debe ser realista.
            2. No recomiendes un monto menor al ahorro mensual mínimo calculado.
            3. No recomiendes reducir gastos esenciales como salud, vivienda
               o alimentación básica, salvo que exista un gasto claramente excesivo.
            4. Selecciona como máximo tres categorías que el usuario podría revisar.
            5. La explicación debe ser breve, clara y estar escrita en español.
            6. No agregues consejos extremos, préstamos ni endeudamiento.
            7. Devuelve únicamente un objeto JSON válido.
            8. No agregues Markdown, comillas triples ni texto antes o después del JSON.

            FORMATO OBLIGATORIO:

            {
              "monthlySaving": 65000.0,
              "categoriesToReduce": [
                "Entretenimiento",
                "Comida rápida"
              ],
              "explanation": "Reduciendo ligeramente estas categorías podrás ahorrar el monto recomendado cada mes sin afectar tus necesidades principales."
            }
        """.trimIndent()
    }

    const val SYSTEM_PROMPT: String =
        """
        Eres un asistente financiero integrado en FinTrack.

        Tu función es generar planes de ahorro claros, prudentes y realistas
        utilizando únicamente la información proporcionada por la aplicación.

        No inventes ingresos, gastos ni categorías.
        No recomiendes préstamos, créditos ni inversiones riesgosas.
        No reemplazas la orientación de un profesional financiero.

        Debes responder únicamente con el JSON solicitado por el usuario.
        """
}