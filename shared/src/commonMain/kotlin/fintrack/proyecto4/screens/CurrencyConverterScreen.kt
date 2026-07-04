package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.montserratFamily
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/** Monedas disponibles en el conversor. CRC (colón) es la moneda base del país. */
private val availableCurrencies = listOf("CRC", "USD", "EUR", "MXN", "GBP")

/** Bandera representativa de cada moneda, para mostrarla junto al código en los chips. */
private val currencyFlags = mapOf(
    "CRC" to "🇨🇷",
    "USD" to "🇺🇸",
    "EUR" to "🇪🇺",
    "MXN" to "🇲🇽",
    "GBP" to "🇬🇧"
)

/** Nombre legible de cada moneda, mostrado como referencia bajo el selector. */
private val currencyNames = mapOf(
    "CRC" to "Colón costarricense",
    "USD" to "Dólar estadounidense",
    "EUR" to "Euro",
    "MXN" to "Peso mexicano",
    "GBP" to "Libra esterlina"
)

/** Símbolo de cada moneda, usado para mostrar el resultado (ej. "$96.15"). */
private val currencySymbols = mapOf(
    "CRC" to "₡",
    "USD" to "$",
    "EUR" to "€",
    "MXN" to "$",
    "GBP" to "£"
)

/**
 * Calcula el resultado de la conversión usando un tipo de cambio ingresado manualmente por el
 * usuario (unidades de CRC por 1 unidad de moneda extranjera, como lo cotiza el BCCR).
 *
 * Si el origen es CRC, el monto se divide entre el tipo de cambio (colones -> divisa). Si el
 * destino es CRC, el monto se multiplica por el tipo de cambio (divisa -> colones).
 */
fun convertWithManualRate(
    amount: Double,
    fromCurrency: String,
    toCurrency: String,
    exchangeRate: Double
): Double = when {
    fromCurrency == toCurrency -> amount
    fromCurrency == "CRC" -> amount / exchangeRate
    toCurrency == "CRC" -> amount * exchangeRate
    else -> amount * exchangeRate
}

/** Formatea un monto con separador de miles (espacio) y 2 decimales. Ej: 52050.0 -> "52 050.00". */
fun formatCurrencyAmount(amount: Double): String = formatDecimal(amount, decimals = 2)

/** Formatea un monto con 2 decimales anteponiendo el símbolo de la moneda. Ej: "$96.15". */
private fun formatAmountWithSymbol(amount: Double, currency: String): String =
    "${currencySymbols[currency] ?: ""}${formatCurrencyAmount(amount)}"

/** Formatea una tasa de cambio con 4 decimales, como en "1 USD = 520.5000 CRC". */
private fun formatExchangeRate(rate: Double): String = formatDecimal(rate, decimals = 4)

/** Describe la tasa manual ingresada en términos de "1 <divisa> = X CRC" cuando aplica. */
private fun exchangeRateDescription(fromCurrency: String, toCurrency: String, rate: Double): String {
    val foreignCurrency = when {
        fromCurrency == "CRC" -> toCurrency
        toCurrency == "CRC" -> fromCurrency
        else -> null
    }
    return if (foreignCurrency != null) {
        "1 $foreignCurrency = ${formatExchangeRate(rate)} CRC"
    } else {
        "Tipo de cambio $fromCurrency → $toCurrency: ${formatExchangeRate(rate)}"
    }
}

private fun formatDecimal(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val isNegative = rounded < 0
    val absValue = abs(rounded)
    val wholePart = absValue.toLong()
    val fractionDigits = round((absValue - wholePart) * factor).toLong().toString().padStart(decimals, '0')

    val groupedWhole = StringBuilder().apply {
        wholePart.toString().reversed().forEachIndexed { index, digit ->
            if (index > 0 && index % 3 == 0) append(' ')
            append(digit)
        }
    }.reverse().toString()

    val sign = if (isNegative) "-" else ""
    return if (decimals > 0) "$sign$groupedWhole.$fractionDigits" else "$sign$groupedWhole"
}

/** Deja pasar solo dígitos y un único separador decimal (acepta '.' o ',' y lo normaliza a '.'). */
private fun sanitizeAmountInput(input: String): String {
    val result = StringBuilder()
    var hasDecimalPoint = false
    for (char in input) {
        when {
            char.isDigit() -> result.append(char)
            (char == '.' || char == ',') && !hasDecimalPoint -> {
                result.append('.')
                hasDecimalPoint = true
            }
        }
    }
    return result.toString()
}

/**
 * Formatea un monto mientras se escribe: agrupa la parte entera con '.' cada 3 dígitos
 * (ej. "1000000" -> "1.000.000") y muestra la parte decimal separada por ',' (ej.
 * "1000000.5" -> "1.000.000,5"), sin alterar el valor sin formato usado para los cálculos.
 */
private object AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dotIndex = raw.indexOf('.')
        val intPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val decimalDigits = if (dotIndex >= 0) raw.substring(dotIndex + 1) else null

        val total = intPart.length
        val map = IntArray(total + 1)
        val groupedInt = StringBuilder().apply {
            for (i in 0 until total) {
                if (i > 0 && (total - i) % 3 == 0) append('.')
                map[i] = length
                append(intPart[i])
            }
            map[total] = length
        }.toString()

        val transformed = if (decimalDigits != null) "$groupedInt,$decimalDigits" else groupedInt

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= total) map[offset.coerceIn(0, total)] else groupedInt.length + (offset - total)

            override fun transformedToOriginal(offset: Int): Int {
                if (offset > groupedInt.length) return total + (offset - groupedInt.length)
                var result = 0
                for (i in 0..total) if (map[i] <= offset) result = i
                return result
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

@Composable
fun CurrencyConverterScreen(
    onBack: () -> Unit = {}
) {
    val montserrat = montserratFamily()

    var amountText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("CRC") }

    // Estos valores se recalculan solos en cada recomposición, es decir, cada vez que
    // cambian monto, tipo de cambio, monedaOrigen o monedaDestino.
    val monto = amountText.toDoubleOrNull() ?: 0.0
    val tipoCambio = rateText.toDoubleOrNull() ?: 0.0
    val amountIsInvalid = amountText.isNotBlank() && monto <= 0.0
    val rateIsInvalid = rateText.isNotBlank() && tipoCambio <= 0.0
    val isValidForResult = monto > 0.0 && tipoCambio > 0.0
    val resultado = if (isValidForResult) convertWithManualRate(monto, fromCurrency, toCurrency, tipoCambio) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
    ) {
        ScreenHeader(title = "Conversor de divisas", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "MONTO", montserrat = montserrat)
                AmountField(
                    value = amountText,
                    onValueChange = { amountText = sanitizeAmountInput(it) },
                    montserrat = montserrat
                )
                if (amountIsInvalid) {
                    FieldErrorText(text = "El monto debe ser mayor a 0", montserrat = montserrat)
                }
            }

            CurrencySelector(
                label = "DE",
                selected = fromCurrency,
                onSelect = { fromCurrency = it },
                montserrat = montserrat
            )

            CurrencySelector(
                label = "A",
                selected = toCurrency,
                onSelect = { toCurrency = it },
                montserrat = montserrat
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "TIPO DE CAMBIO", montserrat = montserrat)
                AmountField(
                    value = rateText,
                    onValueChange = { rateText = sanitizeAmountInput(it) },
                    montserrat = montserrat
                )
                if (rateIsInvalid) {
                    FieldErrorText(text = "El tipo de cambio debe ser mayor a 0", montserrat = montserrat)
                } else {
                    Text(
                        text = "Ingresa manualmente cuántos colones equivalen a 1 unidad de la moneda extranjera",
                        color = FinTrackColors.TextSecondary,
                        fontFamily = montserrat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ConversionResultCard(
                amount = monto,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                result = resultado,
                exchangeRate = tipoCambio
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, montserrat: FontFamily) {
    Text(
        text = text,
        color = FinTrackColors.TextSecondary,
        fontFamily = montserrat,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
}

/** Texto de error de validación bajo un campo (monto o tipo de cambio inválidos). */
@Composable
private fun FieldErrorText(text: String, montserrat: FontFamily) {
    Text(
        text = text,
        color = FinTrackColors.ErrorColor,
        fontFamily = montserrat,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * Chip para seleccionar una moneda; azul cuando está seleccionado, gris oscuro si no.
 * Bandera y código apilados verticalmente para que quepan 5 chips en una sola fila sin scroll,
 * repartiéndose el ancho disponible en partes iguales ([Modifier.weight]).
 */
@Composable
fun CurrencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val montserrat = montserratFamily()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) FinTrackColors.BlueMeta else FinTrackColors.SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = currencyFlags[label] ?: "",
            fontSize = 16.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = if (selected) FinTrackColors.White else FinTrackColors.TextSecondary,
            fontFamily = montserrat,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    selected: String,
    onSelect: (String) -> Unit,
    montserrat: FontFamily
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = label, montserrat = montserrat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableCurrencies.forEach { currency ->
                CurrencyChip(
                    label = currency,
                    selected = currency == selected,
                    onClick = { onSelect(currency) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = currencyNames[selected] ?: selected,
            color = FinTrackColors.TextSecondary,
            fontFamily = montserrat,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    montserrat: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FinTrackColors.SurfaceSecondary)
            .border(1.dp, FinTrackColors.BorderDefault, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = AmountVisualTransformation,
            textStyle = TextStyle(
                color = FinTrackColors.TextPrimary,
                fontFamily = montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(FinTrackColors.BlueMeta),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "0",
                        color = FinTrackColors.WhiteAlpha40,
                        fontFamily = montserrat,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                innerTextField()
            }
        )
    }
}

/** Tarjeta azul grande y centrada con el resultado de la conversión. */
@Composable
fun ConversionResultCard(
    amount: Double,
    fromCurrency: String,
    toCurrency: String,
    result: Double,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val montserrat = montserratFamily()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FinTrackColors.GradientMeta)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${formatAmountWithSymbol(amount, fromCurrency)} $fromCurrency =",
            color = FinTrackColors.White.copy(alpha = 0.85f),
            fontFamily = montserrat,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${formatAmountWithSymbol(result, toCurrency)} $toCurrency",
            color = FinTrackColors.White,
            fontFamily = montserrat,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = exchangeRateDescription(fromCurrency, toCurrency, exchangeRate),
            color = FinTrackColors.White.copy(alpha = 0.75f),
            fontFamily = montserrat,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}