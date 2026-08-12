package fintrack.proyecto4.ai

object SavingsProjectionPrompt {

    const val SYSTEM_PROMPT = """
        Eres un asistente financiero dentro de una aplicación de finanzas personales.

        Tu tarea es explicar de manera clara y breve la proyección de una meta de ahorro.

        Reglas:
        - Utiliza únicamente los datos proporcionados.
        - No inventes fechas, montos ni resultados.
        - No recomiendes préstamos ni endeudamiento.
        - No prometas resultados financieros.
        - Usa un tono motivador, pero realista.
        - Responde en español.
        - Responde únicamente con la explicación.
        - Utiliza como máximo 3 oraciones.
        - No uses listas.
        - No uses Markdown.
        - No devuelvas JSON.
    """

    fun buildPrompt(
        goalName: String,
        remainingAmount: Double,
        projection: SavingsProjection
    ): String {
        return """
            Explica la siguiente proyección de ahorro:

            Nombre de la meta: $goalName
            Monto pendiente: ₡${remainingAmount.toLong()}
            Promedio mensual ahorrado: ₡${projection.averageMonthlySaving.toLong()}
            Ahorro mensual necesario: ₡${projection.requiredMonthlySaving.toLong()}
            Meses restantes hasta la fecha límite: ${projection.monthsRemaining}
            Fecha proyectada de finalización: ${projection.projectedCompletionDate ?: "No disponible"}
            Estado de la proyección: ${projection.status}

            Significado de los estados:
            AHEAD: el usuario podría completar la meta antes de tiempo.
            ON_TRACK: el usuario mantiene un ritmo suficiente.
            BEHIND: el ritmo actual no es suficiente.
            COMPLETED: la meta ya fue completada.
            NO_DATA: todavía no hay suficientes aportes para calcular el ritmo.

            Redacta una explicación clara, breve y útil para el usuario.
        """.trimIndent()
    }
}