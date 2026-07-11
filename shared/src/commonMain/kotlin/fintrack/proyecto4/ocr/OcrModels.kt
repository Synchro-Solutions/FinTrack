package fintrack.proyecto4.ocr


data class OcrResult(
    val merchantName: String?,
    val amount: String?,
    val date: String?,
    val rawText: String
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
