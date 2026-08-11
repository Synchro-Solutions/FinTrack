package fintrack.proyecto4.ai

import kotlinx.serialization.Serializable

/**
 * Resultado de pedirle a la IA que parsee el texto crudo de ML Kit. Todos los campos son
 * nullable a propósito: null significa "la IA no pudo determinar este dato" (se distingue de
 * un dato que el OCR sí detectó pero la IA no corrigió).
 */
@Serializable
data class ReceiptAiResult(
    val monto: Double? = null,
    val fecha: String? = null,
    val comercio: String? = null,
    val categoria: String? = null
)
