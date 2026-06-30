package fintrack.proyecto4.util

/**
 * Formatea un monto en colones costarricenses con separador de miles (espacio).
 * Ejemplo: 781000 → "₡781 000", -5000 → "-₡5 000"
 */
fun formatColones(amount: Long): String {
    val abs = if (amount < 0) -amount else amount
    val str = abs.toString()
    val result = StringBuilder()
    str.reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) result.append(' ')
        result.append(c)
    }
    return "${if (amount < 0) "-" else ""}₡${result.reverse()}"
}

/**
 * Formatea un monto en colones de forma compacta.
 * Ejemplo: 1_000_000 → "₡1.0M", 239_000 → "₡239K"
 */
fun formatColonesCompacto(amount: Long): String = when {
    amount >= 1_000_000 -> {
        val whole = amount / 1_000_000
        val frac  = (amount % 1_000_000) / 100_000
        "₡${whole}.${frac}M"
    }
    amount >= 1_000 -> "₡${amount / 1_000}K"
    else            -> "₡$amount"
}

/**
 * Formatea un porcentaje como string.
 * Ejemplo: 77 → "77%"
 */
fun formatPercent(value: Int): String = "$value%"
