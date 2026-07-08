package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColones

private data class AguinaldoMonth(val label: String)

private val aguinaldoMonths = listOf(
    AguinaldoMonth("Dic anterior"),
    AguinaldoMonth("Enero"),
    AguinaldoMonth("Febrero"),
    AguinaldoMonth("Marzo"),
    AguinaldoMonth("Abril"),
    AguinaldoMonth("Mayo"),
    AguinaldoMonth("Junio"),
    AguinaldoMonth("Julio"),
    AguinaldoMonth("Agosto"),
    AguinaldoMonth("Setiembre"),
    AguinaldoMonth("Octubre"),
    AguinaldoMonth("Noviembre")
)

private object AguinaldoIntroState {
    var alreadyShown: Boolean = false
}

@Composable
fun AguinaldoCalculatorScreen(onBack: () -> Unit = {}) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current
    val amounts = remember { aguinaldoMonths.map { "" }.toMutableStateList() }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var showIntroDialog by remember { mutableStateOf(!AguinaldoIntroState.alreadyShown) }
    val largeTextMode = false

    val values = amounts.map { it.toLongOrNull() ?: 0L }
    val totalSalary = values.sum()
    val estimatedAguinaldo = totalSalary / 12L
    val emptyMonths = amounts.count { it.isBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        ScreenHeader(
            title = "Calculadora de Aguinaldo",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compactLayout = maxHeight < 780.dp
                val ultraCompactLayout = maxHeight < 700.dp
                val lowerBlockSpacing = if (ultraCompactLayout) 2.dp else if (compactLayout) 3.dp else 5.dp

                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Ingresa los salarios ordinarios de los ultimos 12 meses (dic-nov).",
                        color = FinTrackColors.WarningText,
                        fontFamily = montserrat,
                        fontSize = if (ultraCompactLayout) 11.sp else if (largeTextMode) 14.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FinTrackColors.AmberDark.copy(alpha = 0.35f))
                            .border(1.dp, FinTrackColors.WarningColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = if (ultraCompactLayout) 6.dp else if (compactLayout) 8.dp else 10.dp)
                    )

                    Spacer(Modifier.height(if (ultraCompactLayout) 4.dp else if (compactLayout) 6.dp else 8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (ultraCompactLayout) 3.dp else if (compactLayout) 4.dp else 6.dp)
                    ) {
                        aguinaldoMonths.forEachIndexed { index, month ->
                            SalaryRow(
                                monthLabel = month.label,
                                value = amounts[index],
                                onValueChange = { amounts[index] = sanitizeSalaryInput(it) },
                                montserrat = montserrat,
                                largeTextMode = largeTextMode,
                                compactLayout = compactLayout,
                                ultraCompactLayout = ultraCompactLayout
                            )
                        }
                    }

                    Spacer(Modifier.height(lowerBlockSpacing))

                    ValidationSlot(
                        emptyMonths = emptyMonths,
                        montserrat = montserrat,
                        largeTextMode = largeTextMode,
                        compactLayout = compactLayout,
                        ultraCompactLayout = ultraCompactLayout
                    )

                    Spacer(Modifier.height(lowerBlockSpacing))

                    SalaryTotalRow(
                        totalSalary = totalSalary,
                        montserrat = montserrat,
                        largeTextMode = largeTextMode,
                        compactLayout = compactLayout,
                        ultraCompactLayout = ultraCompactLayout,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(formatColones(totalSalary)))
                        }
                    )

                    Spacer(Modifier.height(lowerBlockSpacing))

                    SummaryCard(
                        estimatedAguinaldo = estimatedAguinaldo,
                        totalSalary = totalSalary,
                        montserrat = montserrat,
                        largeTextMode = largeTextMode,
                        compactLayout = compactLayout,
                        ultraCompactLayout = ultraCompactLayout,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(formatColones(estimatedAguinaldo)))
                        }
                    )

                    Spacer(Modifier.height(lowerBlockSpacing))

                    if (!ultraCompactLayout) {
                        Text(
                            text = "Estimado. El aguinaldo real puede incluir otras compensaciones salariales.",
                            color = colors.textSecondary,
                            fontFamily = montserrat,
                            fontSize = if (compactLayout) 9.sp else if (largeTextMode) 11.sp else 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (showIntroDialog) {
            AguinaldoIntroDialog(
                onDismiss = {
                    AguinaldoIntroState.alreadyShown = true
                    showIntroDialog = false
                }
            )
        }
    }
}

@Composable
private fun SalaryRow(
    monthLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    largeTextMode: Boolean,
    compactLayout: Boolean,
    ultraCompactLayout: Boolean
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when {
                    ultraCompactLayout && largeTextMode -> 34.dp
                    ultraCompactLayout -> 30.dp
                    compactLayout && largeTextMode -> 37.dp
                    compactLayout -> 33.dp
                    largeTextMode -> 42.dp
                    else -> 38.dp
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = monthLabel,
            color = colors.textPrimary,
            fontFamily = montserrat,
            fontSize = if (ultraCompactLayout) 12.sp else if (compactLayout) 13.sp else if (largeTextMode) 15.sp else 14.sp,
            modifier = Modifier.width(
                when {
                    ultraCompactLayout -> 88.dp
                    compactLayout -> 94.dp
                    largeTextMode -> 110.dp
                    else -> 100.dp
                }
            )
        )

        AmountInput(
            value = value,
            onValueChange = onValueChange,
            montserrat = montserrat,
            largeTextMode = largeTextMode,
            compactLayout = compactLayout,
            ultraCompactLayout = ultraCompactLayout,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    largeTextMode: Boolean,
    compactLayout: Boolean,
    ultraCompactLayout: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .height(
                when {
                    ultraCompactLayout && largeTextMode -> 34.dp
                    ultraCompactLayout -> 30.dp
                    compactLayout && largeTextMode -> 37.dp
                    compactLayout -> 33.dp
                    largeTextMode -> 42.dp
                    else -> 38.dp
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSecondary)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = ThousandSeparatorVisualTransformation,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = if (ultraCompactLayout) 13.sp else if (compactLayout) 15.sp else if (largeTextMode) 18.sp else 17.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(FinTrackColors.GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "0",
                        color = FinTrackColors.WhiteAlpha40,
                        fontFamily = montserrat,
                        fontSize = if (ultraCompactLayout) 13.sp else if (compactLayout) 15.sp else if (largeTextMode) 18.sp else 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                innerTextField()
            }
        )
    }
}

private object ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = formatThousandsWithDots(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                return formatThousandsWithDots(raw.take(safe)).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, formatted.length)
                return formatted.take(safe).count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

private fun formatThousandsWithDots(digits: String): String {
    if (digits.isEmpty()) return ""
    return digits.reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

private fun sanitizeSalaryInput(input: String): String = input.filter(Char::isDigit)

@Composable
private fun ValidationSlot(
    emptyMonths: Int,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    largeTextMode: Boolean,
    compactLayout: Boolean,
    ultraCompactLayout: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (ultraCompactLayout) 14.dp else if (compactLayout) 16.dp else 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (emptyMonths > 0) {
            Text(
                text = "Faltan $emptyMonths meses por completar.",
                color = FinTrackColors.WarningText,
                fontFamily = montserrat,
                fontSize = if (ultraCompactLayout) 11.sp else if (compactLayout) 12.sp else if (largeTextMode) 14.sp else 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SummaryCard(
    estimatedAguinaldo: Long,
    totalSalary: Long,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    largeTextMode: Boolean,
    compactLayout: Boolean,
    ultraCompactLayout: Boolean,
    onCopy: () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = if (ultraCompactLayout) 4.dp else if (compactLayout) 5.dp else 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aguinaldo estimado",
                    color = FinTrackColors.WarningText,
                    fontFamily = montserrat,
                    fontSize = if (ultraCompactLayout) 12.sp else if (compactLayout) 13.sp else if (largeTextMode) 16.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar aguinaldo",
                        tint = colors.textSecondary,
                        modifier = Modifier.height(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Copiar",
                        color = colors.textSecondary,
                        fontFamily = montserrat,
                        fontSize = if (ultraCompactLayout) 10.sp else if (compactLayout) 11.sp else if (largeTextMode) 13.sp else 12.sp
                    )
                }
            }

            Spacer(Modifier.height(if (ultraCompactLayout) 0.dp else 1.dp))

            Text(
                text = formatColones(estimatedAguinaldo),
                color = colors.textPrimary,
                fontFamily = montserrat,
                fontSize = if (ultraCompactLayout) 19.sp else if (compactLayout) 23.sp else if (largeTextMode) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = if (ultraCompactLayout) 21.sp else if (compactLayout) 25.sp else if (largeTextMode) 34.sp else 30.sp
            )

            if (!ultraCompactLayout) {
                Text(
                    text = "Cálculo: ${formatColones(totalSalary)} ÷ 12",
                    color = colors.textSecondary,
                    fontFamily = montserrat,
                    fontSize = if (compactLayout) 9.sp else if (largeTextMode) 11.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SalaryTotalRow(
    totalSalary: Long,
    montserrat: androidx.compose.ui.text.font.FontFamily,
    largeTextMode: Boolean,
    compactLayout: Boolean,
    ultraCompactLayout: Boolean,
    onCopy: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Suma de salarios: ${formatColones(totalSalary)}",
            color = colors.textSecondary,
            fontFamily = montserrat,
            fontSize = if (ultraCompactLayout) 11.sp else if (compactLayout) 12.sp else if (largeTextMode) 15.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar suma de salarios",
                tint = colors.textSecondary,
                modifier = Modifier.height(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Copiar",
                color = colors.textSecondary,
                fontFamily = montserrat,
                fontSize = if (ultraCompactLayout) 10.sp else if (compactLayout) 11.sp else if (largeTextMode) 13.sp else 12.sp
            )
        }
    }
}

@Composable
private fun AguinaldoIntroDialog(onDismiss: () -> Unit) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Como usar esta calculadora",
                fontFamily = montserrat,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "1) Ingresa salarios ordinarios de dic a nov.\n" +
                        "2) Escribe montos sin centimos.\n" +
                        "3) Puedes copiar el aguinaldo estimado y la suma de salarios.",
                fontFamily = montserrat,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", fontFamily = montserrat, color = FinTrackColors.GreenPrimary)
            }
        }
    )
}
