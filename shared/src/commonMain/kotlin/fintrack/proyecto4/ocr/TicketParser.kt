package fintrack.proyecto4.ocr

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock


object TicketParser {

    fun parse(rawText: String): OcrResult = OcrResult(
        merchantName = extractMerchant(rawText),
        amount = extractAmount(rawText),
        date = extractDate(rawText),
        rawText = rawText
    )

    // ─── Comercio ───────────────────────────────────────────────────────────

    private fun extractMerchant(text: String): String? {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val preferred = lines.take(12).firstOrNull { line ->
            line.contains("PALI", ignoreCase = true) ||
                line.contains("MAXI", ignoreCase = true) ||
                line.contains("RESTAURANTE", ignoreCase = true) ||
                line.contains("FARMACIA", ignoreCase = true) ||
                line.contains("FARMAVALUE", ignoreCase = true) ||
                line.contains("FISCHEL", ignoreCase = true) ||
                line.contains("SUPER MORA", ignoreCase = true) ||
                line.contains("CHICO", ignoreCase = true)
        }

        if (preferred != null) return preferred

        val ignoredWords = listOf(
            "CORP", "S.R.L", "S.A", "CED", "JUR", "TEL", "TELEFONO", "FAX",
            "CLIENTE", "CAJERO", "MESERO", "FECHA", "HORA", "FACTURA",
            "TIQUETE", "ELECTRONICO", "TOTAL", "SUBTOTAL", "IVA", "IMPUESTO",
            "CLAVE", "SERVICIO", "CONSULTE", "WWW", "HTTP", "CAJA", "OP#",
            "TR#", "TE#", "TDA#"
        )

        return lines.take(10).firstOrNull { line ->
            line.length >= 4 &&
                line.any { it.isLetter() } &&
                ignoredWords.none { ignored -> line.contains(ignored, ignoreCase = true) }
        }
    }

    // ─── Fecha ──────────────────────────────────────────────────────────────

    private val DATE_KEYWORDS = listOf("FECHA", "FEC", "DATE", "EMISION", "EMISIÓN")

    // día/mes/año con separador /, - o . (acepta año de 2 o 4 dígitos).
    private val DMY_DATE_REGEX = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4}|\d{2})""")

    // año/mes/día estilo ISO (siempre año de 4 dígitos primero).
    private val YMD_DATE_REGEX = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""")

    private const val MIN_VALID_YEAR = 2000

    private fun extractDate(text: String): String? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Prioridad 1: la línea que menciona una palabra clave de fecha (y, por si el
        // OCR separó etiqueta y valor en líneas distintas, también la línea siguiente).
        val keywordIndex = lines.indexOfFirst { line -> DATE_KEYWORDS.any { line.contains(it, ignoreCase = true) } }
        if (keywordIndex != -1) {
            lines.drop(keywordIndex).take(2)
                .firstNotNullOfOrNull { findDateInLine(it) }
                ?.let { return it }
        }

        // Prioridad 2: cualquier línea del comprobante con un patrón de fecha reconocible.
        return lines.firstNotNullOfOrNull { findDateInLine(it) }
    }

    private fun findDateInLine(line: String): String? {
        // Se intenta primero el formato ISO (año de 4 dígitos primero) para evitar que
        // el patrón día/mes/año interprete mal una fecha ISO (p.ej. "2025-07-27" no debe
        // leerse como día=25, mes=07, año=27).
        YMD_DATE_REGEX.find(line)?.let { match ->
            val (year, month, day) = match.destructured
            normalizeDate(year = year, month = month, day = day)?.let { return it }
        }
        DMY_DATE_REGEX.find(line)?.let { match ->
            val (day, month, year) = match.destructured
            normalizeDate(year = year, month = month, day = day)?.let { return it }
        }
        return null
    }


    private fun normalizeDate(year: String, month: String, day: String): String? {
        val yearInt = expandYear(year) ?: return null
        val monthInt = month.toIntOrNull() ?: return null
        val dayInt = day.toIntOrNull() ?: return null

        if (yearInt !in MIN_VALID_YEAR..maxPlausibleYear()) return null

        val date = try {
            LocalDate(yearInt, monthInt, dayInt)
        } catch (e: IllegalArgumentException) {
            return null
        }

        val dd = date.day.toString().padStart(2, '0')
        val mm = date.month.number.toString().padStart(2, '0')
        return "$dd/$mm/${date.year}"
    }

    private fun expandYear(raw: String): Int? {
        val value = raw.toIntOrNull() ?: return null
        return if (raw.length == 2) 2000 + value else value
    }

    private fun maxPlausibleYear(): Int =
        Clock.System.todayIn(TimeZone.currentSystemDefault()).year + 1

    private val PRIMARY_AMOUNT_KEYWORDS = listOf(
        "TOTAL A PAGAR",
        "MONTO TOTAL",
        "IMPORTE TOTAL",
        "TOTAL VENTA",
        "GRAN TOTAL",
        "TOTAL CON PROPINA",
        "TOTAL FACTURA",
        "NETO A PAGAR",
        "TOTAL",
        "IMPORTE",
        "INPORTE",
        "PAGO",
        "MONTO"
    )

    // SUBTOTAL solo se usa como último recurso antes del escaneo genérico, nunca primero.
    private val SECONDARY_AMOUNT_KEYWORDS = listOf("SUBTOTAL")

    // Líneas que casi seguro NO contienen el monto total, aunque tengan números grandes:
    // identificadores (cédula, autorización, caja, consecutivo), fecha/hora, y desgloses
    // (descuento/impuesto) que no deben confundirse con el total si hay uno mejor.
    // Nota: "FACTURA" se excluye a propósito de esta lista (y no "número de factura" en
    // general) porque "TOTAL FACTURA" es uno de los keywords primarios válidos; el
    // consecutivo/número de factura igual queda cubierto por CONSECUTIVO + los rangos
    // de plausibilidad y la búsqueda acotada a la ventana del keyword.
    private val AMOUNT_EXCLUDED_LINE_KEYWORDS = listOf(
        "TEL", "TELEFONO", "TELÉFONO", "FAX",
        "CED", "CÉDULA", "CEDULA", "JURIDICA", "JURÍDICA",
        "CONSECUTIVO", "AUTORIZ", "CAJA", "TERMINAL",
        "LOTE", "APROBACION", "APROBACIÓN", "REF", "CLAVE",
        "FECHA", "HORA",
        "DESCUENTO", "IMPUESTO", "IVA"
    )

    // Token numérico completo (dígitos + separadores mixtos), sin asumir un formato fijo;
    // la normalización decide si el separador final es de miles o decimal.
    private val AMOUNT_TOKEN_REGEX = Regex("""\d[\d.,]*\d|\d""")

    private const val MIN_PLAUSIBLE_AMOUNT = 100L
    private const val MAX_PLAUSIBLE_AMOUNT = 100_000_000L

    private fun extractAmount(text: String): String? {
        val allLines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidateLines = allLines.filter { line ->
            AMOUNT_EXCLUDED_LINE_KEYWORDS.none { keyword -> line.contains(keyword, ignoreCase = true) }
        }

        // "SUBTOTAL" contiene la palabra "TOTAL" como substring: se excluye explícitamente
        // de la búsqueda primaria para que el keyword genérico "TOTAL" nunca la confunda
        // con el total real (SUBTOTAL solo se considera en la búsqueda secundaria).
        val primaryCandidateLines = candidateLines.filterNot { it.contains("SUBTOTAL", ignoreCase = true) }

        findAmountNearKeywords(primaryCandidateLines, PRIMARY_AMOUNT_KEYWORDS)?.let { return it }
        findAmountNearKeywords(candidateLines, SECONDARY_AMOUNT_KEYWORDS)?.let { return it }

        return findPlausibleAmountAnywhere(candidateLines)
    }

    private fun findAmountNearKeywords(lines: List<String>, keywords: List<String>): String? {
        for (keyword in keywords) {
            val index = lines.indexOfFirst { it.contains(keyword, ignoreCase = true) }
            if (index == -1) continue

            // Prioriza el monto en la misma línea que la palabra clave; si no aparece ahí,
            // busca en las 2 líneas siguientes (el OCR suele separar etiqueta y valor).
            val window = listOf(lines[index]) + lines.drop(index + 1).take(2)
            val best = plausibleAmountsIn(window).maxOrNull()

            if (best != null) return best.toString()
        }
        return null
    }

    private fun findPlausibleAmountAnywhere(lines: List<String>): String? =
        plausibleAmountsIn(lines).maxOrNull()?.toString()

    private fun plausibleAmountsIn(lines: List<String>): List<Long> =
        lines
            .flatMap { line -> AMOUNT_TOKEN_REGEX.findAll(line).map { it.value } }
            .mapNotNull { normalizeAmount(it) }
            .mapNotNull { it.toLongOrNull() }
            .filter { it in MIN_PLAUSIBLE_AMOUNT..MAX_PLAUSIBLE_AMOUNT }

    /**
     * Normaliza un token numérico crudo (p.ej. "45,000.00", "45.000,00", "5990") a solo
     * dígitos enteros (formato usado por TransactionFormState.amount, sin centavos).
     *
     * Regla de desambiguación: si hay un separador (',' o '.') y el último grupo tras
     * ese separador tiene 1-2 dígitos, se interpreta como decimal (se descarta, ya que
     * el monto de la app es en colones enteros); si tiene 3 dígitos, se interpreta como
     * separador de miles (se conserva como parte del entero).
     */
    private fun normalizeAmount(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null

        val lastSeparatorIndex = maxOf(cleaned.lastIndexOf(','), cleaned.lastIndexOf('.'))

        val integerDigits = if (lastSeparatorIndex == -1) {
            cleaned.filter { it.isDigit() }
        } else {
            val fractionalPart = cleaned.substring(lastSeparatorIndex + 1)
            val looksLikeDecimals = fractionalPart.length in 1..2
            if (looksLikeDecimals) {
                cleaned.substring(0, lastSeparatorIndex).filter { it.isDigit() }
            } else {
                cleaned.filter { it.isDigit() }
            }
        }

        val trimmed = integerDigits.trimStart('0')
        return when {
            integerDigits.isBlank() -> null
            trimmed.isEmpty() -> "0"
            else -> trimmed
        }
    }
}
