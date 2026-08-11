package fintrack.proyecto4.ai

object ReceiptPrompt {

    fun buildPrompt(
        rawText: String,
        categories: List<String>,
        todayDate: String
    ): String {
        val categoryList = categories.joinToString(separator = ", ")

        return """
            Analiza el texto crudo extraído por OCR de un comprobante de compra (factura,
            tiquete o recibo) y devuelve los datos de la transacción en JSON.

            FECHA DE HOY (referencia para fechas relativas o ambiguas): $todayDate

            CATEGORÍAS DISPONIBLES (elige una, exactamente como está escrita, o null si
            ninguna aplica claramente):
            $categoryList

            TEXTO CRUDO DEL OCR:
            ---
            $rawText
            ---

            INSTRUCCIONES:
            1. El OCR comete errores de reconocimiento (ej. "0" leído como "O", "1" como "I",
               separadores de miles/decimales confusos). Corrígelos usando el contexto de un
               comprobante de compra real.
            2. "monto" es el total final pagado (no subtotal, no vuelto, no propina por
               separado si ya está incluida en el total), en colones, sin símbolo ni
               separadores de miles, como número.
            3. "fecha" debe ir en formato dd/mm/yyyy. Si el comprobante no trae año, o trae
               una fecha relativa, usa la fecha de hoy como referencia.
            4. "comercio" es el nombre del negocio/establecimiento, no el nombre del cliente
               ni del cajero.
            5. "categoria" debe ser exactamente uno de los valores de la lista de categorías
               disponibles, elegido según el tipo de comercio detectado (ej. un supermercado
               es "Alimentación", una farmacia es "Salud"). Si no hay suficiente información
               para elegir con confianza, usa null.
            6. Si no puedes determinar un campo con confianza, usa null para ese campo — no
               inventes datos.
            7. Devuelve únicamente un objeto JSON válido, sin Markdown ni texto adicional.

            FORMATO OBLIGATORIO:

            {
              "monto": 5990,
              "fecha": "27/07/2026",
              "comercio": "Farmacia Fischel",
              "categoria": "Salud"
            }
        """.trimIndent()
    }

    const val SYSTEM_PROMPT: String =
        """
        Eres el asistente OCR de FinTrack, una app de finanzas personales en Costa Rica
        (colones, ₡).

        Tu función es leer el texto crudo de un comprobante de compra (extraído por OCR, con
        posibles errores de reconocimiento) y devolver monto, fecha, comercio y categoría
        sugerida en JSON estructurado.

        No inventes datos que no estén respaldados por el texto. Cuando no tengas certeza,
        usa null en ese campo en vez de adivinar.

        Debes responder únicamente con el JSON solicitado por el usuario.
        """
}
