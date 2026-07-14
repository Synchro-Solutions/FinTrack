package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.dashboard.DashboardViewModel
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.dashboard.MetaItem
import fintrack.proyecto4.dashboard.MonthlyChartData
import fintrack.proyecto4.dashboard.MovimientoItem
import fintrack.proyecto4.dashboard.PresupuestoItem
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.util.formatColones
import fintrack.proyecto4.util.formatColonesCompacto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onNavigateToIngreso: () -> Unit = {},
    onNavigateToGasto: () -> Unit = {},
    onNavigateToOcr: () -> Unit = {},
    onNavigateToReportes: () -> Unit = {},
    onNavigateToAjustes: () -> Unit = {},
    onNavigateToMovimientos: () -> Unit = {},
    onNavigateToPresupuestos: () -> Unit = {},
    onNavigateToMetas: () -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) {
        DashboardViewModel(transactionRepository, uid, onboardingRepository, budgetRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = LocalAppColors.current
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize().background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                DashboardHeader(
                    userName = state.userName,
                    notificationCount = state.notificationCount,
                    onBellClick = { viewModel.marcarNotificacionesLeidas() },
                    onAvatarClick = onNavigateToAjustes
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                BalanceCard(
                    mesActual    = state.mesActual,
                    balance      = state.kpis.balance,
                    ingresos     = state.kpis.ingresos,
                    gastos       = state.kpis.gastos,
                    ahorro       = state.kpis.ahorroPercent,
                    saldoVisible = state.saldoVisible,
                    onToggle     = { viewModel.toggleSaldoVisible() }
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
            item {
                QuickActionsRow(
                    onIngreso = onNavigateToIngreso,
                    onGasto = onNavigateToGasto,
                    onOcr = onNavigateToOcr,
                    onReportes = onNavigateToReportes
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
            item { ChartSection(data = state.chartData) }
            item { Spacer(Modifier.height(24.dp)) }
            item { SectionHeader("Presupuestos", "Ver todos") { onNavigateToPresupuestos() } }
            item { Spacer(Modifier.height(12.dp)) }
            if (state.presupuestos.isEmpty()) {
                item { EmptyPresupuestosState(onNavigateToPresupuestos) }
            } else {
                items(state.presupuestos) { PresupuestoCard(it) }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item { SectionHeader("Meta principal", "Ver metas") { onNavigateToMetas() } }
            item { Spacer(Modifier.height(12.dp)) }
            val meta = state.metaPrincipal
            if (meta == null) {
                item { EmptyMetaState(onNavigateToMetas) }
            } else {
                item { MetaCard(meta) }
            }
            item { Spacer(Modifier.height(16.dp)) }
            if (state.consejoFinanciero.isNotBlank()) {
                item { ConsejoCard(state.consejoFinanciero) }
                item { Spacer(Modifier.height(24.dp)) }
            }
            item { SectionHeader("Últimos movimientos", "Ver todos") { onNavigateToMovimientos() } }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (state.ultimosMovimientos.isEmpty()) {
                        EmptyMovimientosState()
                    } else {
                        state.ultimosMovimientos.forEachIndexed { i, mov ->
                            MovimientoRow(mov)
                            if (i < state.ultimosMovimientos.lastIndex) {
                                HorizontalDivider(color = colors.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/* Header */

@Composable
private fun DashboardHeader(
    userName: String,
    notificationCount: Int,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Hola,", color = colors.textSecondary, fontSize = 13.sp, fontFamily = montserrat)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    userName, color = colors.textPrimary, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, fontFamily = montserrat
                )
                Spacer(Modifier.width(6.dp))
                Text("👋", fontSize = 20.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(badge = {
                if (notificationCount > 0) Badge(containerColor = FinTrackColors.ErrorColor) {
                    Text(if (notificationCount > 9) "9+" else "$notificationCount", fontSize = 9.sp, color = Color.White)
                }
            }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceSecondary, CircleShape)
                        .clickable(onClick = onBellClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)),
                        CircleShape
                    )
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = montserratFamily()
                )
            }
        }
    }
}

/* Tarjeta de saldo */

@Composable
private fun BalanceCard(
    mesActual: String, balance: Long, ingresos: Long,
    gastos: Long, ahorro: Int, saldoVisible: Boolean, onToggle: () -> Unit
) {
    val montserrat = montserratFamily()
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(FinTrackColors.GradientBalance)
        ) {
            Box(
                Modifier.size(200.dp).offset(x = 140.dp, y = (-60).dp)
                    .background(Color.White.copy(alpha = 0.04f), CircleShape)
            )
            Box(
                Modifier.size(140.dp).offset(x = 160.dp, y = 60.dp)
                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
            )

            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Saldo disponible — $mesActual",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp, fontFamily = montserrat
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .clickable(onClick = onToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (saldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null, tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (balance < 0) {
                    Text(
                        if (saldoVisible) formatColones(balance) else "₡••• •••",
                        color = FinTrackColors.ErrorColor, fontSize = 36.sp,
                        fontWeight = FontWeight.Black, fontFamily = montserrat
                    )
                } else {
                    Text(
                        if (saldoVisible) formatColones(balance) else "₡••• •••",
                        color = Color.White, fontSize = 36.sp,
                        fontWeight = FontWeight.Black, fontFamily = montserrat
                    )
                }
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KpiPill(
                        label = "Ingresos",
                        value = if (saldoVisible) formatColonesCompacto(ingresos) else "₡•••",
                        icon = Icons.Default.TrendingUp,
                        iconColor = FinTrackColors.GreenLight
                    )
                    KpiPill(
                        label = "Gastos",
                        value = if (saldoVisible) formatColonesCompacto(gastos) else "₡•••",
                        icon = Icons.Default.TrendingDown,
                        iconColor = FinTrackColors.RedLight
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ahorro", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = montserrat)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("$ahorro%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiPill(label: String, value: String, icon: ImageVector, iconColor: Color) {
    val montserrat = montserratFamily()
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = montserrat)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
        }
    }
}

/* Accesos rápidos */

@Composable
private fun QuickActionsRow(
    onIngreso: () -> Unit, onGasto: () -> Unit,
    onOcr: () -> Unit, onReportes: () -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val actions = listOf(
        Triple("Ingreso",   Icons.Default.TrendingUp,  FinTrackColors.GradientGreen),
        Triple("Gasto",     Icons.Default.TrendingDown, FinTrackColors.GradientRed),
        Triple("OCR",       Icons.Default.CameraAlt,    FinTrackColors.GradientIndigo),
        Triple("Reportes",  Icons.Default.TrendingUp,   FinTrackColors.GradientViolet)
    )
    val callbacks = listOf(onIngreso, onGasto, onOcr, onReportes)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.zip(callbacks).forEach { (action, cb) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = cb)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(action.third),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(action.second, contentDescription = action.first, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(7.dp))
                Text(action.first, color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* Gráfica */

@Composable
private fun ChartSection(data: List<MonthlyChartData>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ingresos vs Gastos", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
            Text("Últimos 6 meses", color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(FinTrackColors.GreenPrimary, "Ingresos")
            LegendDot(FinTrackColors.ErrorColor, "Gastos")
        }
        Spacer(Modifier.height(16.dp))
        BarChart(data)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserratFamily())
    }
}

@Composable
private fun BarChart(data: List<MonthlyChartData>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val maxVal = data.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.toFloat() ?: 1f
    val maxH = 90.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(maxH)
                ) {
                    GradientBar(fraction = item.ingresos / maxVal, width = 11.dp, brush = FinTrackColors.GradientGreenV)
                    GradientBar(fraction = item.gastos / maxVal, width = 11.dp, brush = FinTrackColors.GradientRedV)
                }
                Spacer(Modifier.height(6.dp))
                Text(item.mes, color = colors.textSecondary, fontSize = 10.sp, fontFamily = montserrat)
            }
        }
    }
}

@Composable
private fun GradientBar(fraction: Float, width: Dp, brush: Brush) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(fraction.coerceIn(0.03f, 1f))
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
            .background(brush)
    )
}

/* Presupuestos */

private val presupuestoEmojis = mapOf(
    "Alimentación" to "🛒", "Transporte" to "🚗",
    "Entretenimiento" to "🎬", "Salud" to "💊", "Educación" to "📚"
)

@Composable
private fun PresupuestoCard(item: PresupuestoItem) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val emoji = presupuestoEmojis[item.nombre] ?: "💰"
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.nombre, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
                    Text(
                        "${formatColones(item.gastado)} de ${formatColones(item.total)}",
                        color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat
                    )
                }
                Text(
                    "${item.porcentaje}%",
                    color = item.color, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, fontFamily = montserrat
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.porcentaje / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(item.color.copy(alpha = 0.7f), item.color))
                        )
                )
            }
        }
    }
}

/* Meta */

@Composable
private fun MetaCard(item: MetaItem) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(colors.bg, colors.surfaceSecondary))
            )
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(19.dp))
                .background(colors.surface)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    FinTrackColors.GradientMeta,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) { Text(item.descripcion.ifEmpty { "⭐" }, fontSize = 22.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.nombre, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
                            Text("Vence: ${item.fechaVencimiento}", color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(FinTrackColors.ErrorColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(item.prioridad, color = FinTrackColors.ErrorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatColones(item.ahorrado), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
                    Text("${item.porcentaje}%", color = FinTrackColors.GreenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        .background(FinTrackColors.GreenPrimary.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.porcentaje / 100f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(FinTrackColors.GreenDark, FinTrackColors.GreenLight))
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("ahorrados", color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
            }
        }
    }
}

/* Consejo financiero */

@Composable
private fun ConsejoCard(consejo: String) {
    val montserrat = montserratFamily()
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FinTrackColors.GradientAmber)
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(FinTrackColors.WarningColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("⚡", fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Consejo financiero", color = FinTrackColors.WarningLight, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
                Spacer(Modifier.height(4.dp))
                Text(consejo, color = FinTrackColors.WarningText, fontSize = 12.sp, fontFamily = montserrat, lineHeight = 18.sp)
            }
        }
    }
}

/* Estados vacíos */

@Composable
private fun EmptyPresupuestosState(onNavigate: () -> Unit) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("💰", fontSize = 32.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Sin presupuestos activos",
                color = colors.textPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = montserrat
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Crea tu primer presupuesto y controla\ncuánto gastas en cada categoría.",
                color = colors.textSecondary, fontSize = 12.sp,
                fontFamily = montserrat, textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FinTrackColors.GradientGreen)
                    .clickable(onClick = onNavigate)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Crear presupuesto",
                    color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = montserrat
                )
            }
        }
    }
}

@Composable
private fun EmptyMetaState(onNavigate: () -> Unit) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("🎯", fontSize = 32.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Sin metas definidas",
                color = colors.textPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = montserrat
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Agrega una meta de ahorro y mantén\nel enfoque en lo que más importa.",
                color = colors.textSecondary, fontSize = 12.sp,
                fontFamily = montserrat, textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FinTrackColors.GradientMeta)
                    .clickable(onClick = onNavigate)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Agregar meta",
                    color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = montserrat
                )
            }
        }
    }
}

/* Movimientos */

@Composable
private fun EmptyMovimientosState() {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Aún no tienes movimientos",
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserrat
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Registra tu primer ingreso o gasto para verlo aquí.",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontFamily = montserrat,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MovimientoRow(item: MovimientoItem) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val accentColor = if (item.esIngreso) FinTrackColors.GreenPrimary else FinTrackColors.ErrorColor

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.esIngreso) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.nombre, color = colors.textPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, fontFamily = montserrat,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.categoria} · ${item.fecha}", color = colors.textSecondary,
                fontSize = 11.sp, fontFamily = montserrat
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (item.esIngreso) "+" else "-"}${formatColones(item.monto)}",
                color = accentColor, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = montserrat
            )
            Text(
                if (item.esIngreso) "Ingreso" else "Gasto",
                color = accentColor.copy(alpha = 0.6f),
                fontSize = 10.sp, fontFamily = montserrat
            )
        }
    }
}

/* Componentes base */

@Composable
private fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(actionText, color = FinTrackColors.GreenPrimary, fontSize = 12.sp, fontFamily = montserrat, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = FinTrackColors.GreenPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DarkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}


