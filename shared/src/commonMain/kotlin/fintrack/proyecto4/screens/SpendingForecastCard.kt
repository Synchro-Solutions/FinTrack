package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.ai.CategoryForecast
import fintrack.proyecto4.ai.ForecastState
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.ShimmerText
import fintrack.proyecto4.theme.glassSurface
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColones

@Composable
fun SpendingForecastSection(
    state: ForecastState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    var collapsed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!state.hasRequested) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenDark)
                ) {
                    Text(
                        "🔮  Predecir gastos del próximo mes",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = montserrat
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.glassSurface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { collapsed = !collapsed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔮", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Predicción del próximo mes",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserrat,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (collapsed) "Expandir" else "Colapsar",
                    tint = colors.textSecondary
                )
            }

            if (collapsed) return@Column

            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = FinTrackColors.GreenPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        ShimmerText(
                            text = "Calculando tu predicción…",
                            baseColor = colors.textSecondary,
                            accentColor = colors.primary,
                            fontSize = 13.sp,
                            fontFamily = montserrat
                        )
                    }
                }

                state.insufficientHistory -> {
                    Text(
                        "Necesitas al menos 2 meses de historial para ver la predicción. " +
                            "Por ahora llevas ${state.monthsAvailable} mes${if (state.monthsAvailable == 1) "" else "es"} con gastos registrados.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontFamily = montserrat
                    )
                }

                state.error != null -> {
                    Text(
                        state.error,
                        color = FinTrackColors.ErrorColor,
                        fontSize = 13.sp,
                        fontFamily = montserrat
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRegenerate) {
                        Text("Reintentar", color = FinTrackColors.GreenPrimary, fontFamily = montserrat)
                    }
                }

                state.forecasts.isEmpty() -> {
                    Text(
                        "No hay suficientes gastos por categoría para predecir.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = montserrat
                    )
                }

                else -> {
                    state.forecasts.forEachIndexed { i, f ->
                        ForecastRow(f)
                        if (i < state.forecasts.lastIndex) {
                            HorizontalDivider(
                                color = colors.divider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onRegenerate) {
                        Text("Regenerar", color = colors.textSecondary, fontFamily = montserrat)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(f: CategoryForecast) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = f.categoria,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = montserrat
            )
            Text(
                text = formatColones(f.prediccion),
                color = if (f.excedePresupuesto) FinTrackColors.ErrorColor else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
        }

        if (f.presupuesto != null) {
            Spacer(Modifier.height(2.dp))
            if (f.excedePresupuesto) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FinTrackColors.ErrorColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠ Supera el presupuesto (${formatColones(f.presupuesto)})",
                        color = FinTrackColors.ErrorColor,
                        fontSize = 11.sp,
                        fontFamily = montserrat
                    )
                }
            } else {
                Text(
                    text = "Dentro del presupuesto (${formatColones(f.presupuesto)})",
                    color = FinTrackColors.GreenPrimary,
                    fontSize = 11.sp,
                    fontFamily = montserrat
                )
            }
        }

        if (f.comentario.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = f.comentario,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = montserrat
            )
        }
    }
}
