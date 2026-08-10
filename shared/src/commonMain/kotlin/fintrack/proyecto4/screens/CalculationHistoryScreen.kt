package fintrack.proyecto4.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.history.CalculationHistoryRepository
import fintrack.proyecto4.history.CalculationHistoryViewModel
import fintrack.proyecto4.history.CalculationType
import fintrack.proyecto4.history.NoOpCalculationHistoryRepository
import fintrack.proyecto4.history.SavedCalculation
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.ShimmerText
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColones
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun CalculationHistoryScreen(
    calculationHistoryRepository: CalculationHistoryRepository = NoOpCalculationHistoryRepository(),
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = "historial_$uid") {
        CalculationHistoryViewModel(calculationHistoryRepository, uid)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .size(24.dp)
                        .clickable(onClick = onBack)
                )
            }

            item {
                ShimmerText(
                    text = "Historial",
                    baseColor = colors.textPrimary,
                    accentColor = colors.primary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                TipoSelector(
                    seleccionado = state.tipoSeleccionado,
                    onSeleccionar = { viewModel.onTipoSeleccionado(it) }
                )
            }

            if (!state.isLoading) {
                if (state.calculacionesFiltradas.isEmpty()) {
                    item {
                        EmptyHistoryState(modifier = Modifier.fillParentMaxWidth().padding(top = 40.dp))
                    }
                } else {
                    items(state.calculacionesFiltradas, key = { it.id }) { calculo ->
                        CalculationCard(
                            calculation = calculo,
                            expanded = expandedId == calculo.id,
                            onToggleExpand = {
                                expandedId = if (expandedId == calculo.id) null else calculo.id
                            },
                            onDelete = { pendingDeleteId = calculo.id },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("¿Eliminar este cálculo?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(id)
                    pendingDeleteId = null
                }) {
                    Text("Eliminar", color = FinTrackColors.ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ── Selector de tipo: misma capsula animada que el selector de periodo de Reportes ──

@Composable
private fun TipoSelector(
    seleccionado: CalculationType?,
    onSeleccionar: (CalculationType?) -> Unit
) {
    val colors = LocalAppColors.current
    val opciones: List<CalculationType?> = listOf(null) + CalculationType.entries
    val selectedIndex = opciones.indexOf(seleccionado)

    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / opciones.size
        val offsetX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "tipoPillOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(FinTrackColors.GreenPrimary.copy(alpha = 0.16f))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            opciones.forEach { tipo ->
                val isSelected = tipo == seleccionado
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) FinTrackColors.GreenPrimary else colors.textSecondary,
                    animationSpec = tween(durationMillis = 320),
                    label = "tipoTextColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSeleccionar(tipo) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tipo?.label?.let { shortLabel(it) } ?: "Todos",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = montserratFamily(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Etiquetas cortas para que las 5 opciones quepan en una sola fila sin scroll. */
private fun shortLabel(label: String): String = when (label) {
    "Salario neto" -> "Salario"
    else -> label
}

// ── Tarjeta de cálculo guardado ──────────────────────────────────────────────

@Composable
private fun CalculationCard(
    calculation: SavedCalculation,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(calculation.tipo.label, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
                Text(formatFecha(calculation.fechaCalculo), color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
            }
            calculation.montoPrincipal?.let { monto ->
                Text(formatColones(monto), color = FinTrackColors.GreenPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(calculation.resumen, color = colors.textSecondary, fontSize = 12.sp, fontFamily = montserrat)

        if (expanded && calculation.detalle.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            calculation.detalle.forEach { (label, valor) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat, modifier = Modifier.weight(1f))
                    Text(valor, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = montserrat)
                }
            }
        }

        if (calculation.detalle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onToggleExpand, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = if (expanded) "Ocultar detalle" else "Ver detalle",
                    color = FinTrackColors.GreenPrimary,
                    fontSize = 12.sp,
                    fontFamily = montserrat,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = FinTrackColors.GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatFecha(epochMillis: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val day = date.day.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

// ── Estado vacío ───────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🗂️", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Sin cálculos guardados",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Guardá un resultado desde Aguinaldo, Salario Neto, Vacaciones o Liquidación para verlo acá.",
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}
