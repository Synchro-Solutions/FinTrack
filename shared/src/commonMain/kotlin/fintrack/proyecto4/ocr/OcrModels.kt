package fintrack.proyecto4.ocr


data class OcrResult(
    val merchantName: String?,
    val amount: String?,
    val date: String?,
    val rawText: String,
    /** Categoría sugerida por la IA (debe ser un valor exacto de la lista de categorías
     *  disponibles); null si la IA no corrió o no pudo sugerir ninguna. */
    val suggestedCategory: String? = null,
    /** true si la llamada a la IA se completó (con o sin datos por campo). Distingue "la IA
     *  revisó este campo y no pudo determinarlo" (borde amarillo) de "la IA nunca corrió,
     *  ej. sin conexión" (borde rojo, mismo comportamiento que antes de esta función). */
    val aiEnhanced: Boolean = false,
    val amountFromAi: Boolean = false,
    val dateFromAi: Boolean = false,
    val merchantFromAi: Boolean = false
) {
    companion object {
        fun empty() = OcrResult(merchantName = null, amount = null, date = null, rawText = "")
    }
}

enum class OcrAssistantStatus {
    Idle,
    Processing,
    Success,
    Error
}

data class OcrAssistantUiState(
    val status: OcrAssistantStatus = OcrAssistantStatus.Idle,
    val imagePath: String? = null,
    val result: OcrResult? = null,
    val errorMessage: String? = null
)
