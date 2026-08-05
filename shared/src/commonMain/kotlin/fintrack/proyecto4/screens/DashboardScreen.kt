package fintrack.proyecto4.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import fintrack.proyecto4.ai.AnomalyAlert
import fintrack.proyecto4.ai.AnomalyAlertBus
import fintrack.proyecto4.ai.MonthlySummaryState
import fintrack.proyecto4.ai.MonthlySummaryViewModel
import fintrack.proyecto4.ai.WeeklyAnomalyViewModel
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
import fintrack.proyecto4.theme.ShimmerText
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.theme.shimmerBorderBrush
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.util.formatColones
import fintrack.proyecto4.util.formatColonesCompacto
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onNavigateToOcr: () -> Unit = {},
    onNavigateToAjustes: () -> Unit = {},
    onNavigateToMovimientos: () -> Unit = {},
    onNavigateToPresupuestos: () -> Unit = {},
    onNavigateToMetas: () -> Unit = {},
    onShareText: (String) -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = uid) {
        DashboardViewModel(transactionRepository, uid, onboardingRepository, budgetRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val summaryViewModel = viewModel(key = "summary_$uid") {
        MonthlySummaryViewModel(transactionRepository, uid)
    }
    val summaryState by summaryViewModel.state.collectAsStateWithLifecycle()
    var showAiSummarySheet by remember { mutableStateOf(false) }

    val anomalyAlert by AnomalyAlertBus.current.collectAsStateWithLifecycle()
    val weeklyViewModel = viewModel(key = "weekly_$uid") {
        WeeklyAnomalyViewModel(transactionRepository, uid)
    }
    val weeklyState by weeklyViewModel.state.collectAsStateWithLifecycle()

    // Recarga al volver de otra pantalla (ej. Editar Perfil) para no mostrar
    // nombre/foto/saldos obsoletos: el ViewModel queda cacheado por uid mientras
    // vive la app, y su carga inicial solo corre una vez. Usa loadDashboard() (no
    // refresh()) a proposito: refresh() prende isRefreshing, que dispara el spinner
    // visible de pull-to-refresh en CADA entrada a la pantalla, aunque los datos
    // viejos ya cacheados sigan mostrandose debajo — se siente como que "vuelve a
    // cargar todo" cuando en realidad los datos casi nunca cambiaron. loadDashboard()
    // hace la misma revalidacion en silencio, sin spinner; isRefreshing/el spinner
    // queda reservado para cuando el usuario arrastra a proposito para refrescar.
    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    val colors = LocalAppColors.current
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                DashboardHeader(
                    userName = state.userName,
                    fotoUrl = state.fotoUrl,
                    notificationCount = state.notificationCount,
                    onBellClick = { viewModel.marcarNotificacionesLeidas() },
                    onCameraClick = onNavigateToOcr,
                    onAvatarClick = onNavigateToAjustes,
                    onAiSummaryClick = { showAiSummarySheet = true }
                )
            }
            item { Spacer(Modifier.height(4.dp)) }

            anomalyAlert?.let { alert ->
                item {
                    AnomalyBanner(
                        alert = alert,
                        onDismiss = { AnomalyAlertBus.dismiss() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            if (weeklyState.anomalies.isNotEmpty()) {
                item { WeeklyAnomalyCard(anomalies = weeklyState.anomalies) }
                item { Spacer(Modifier.height(12.dp)) }
            }
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
            item { Spacer(Modifier.height(24.dp)) }
            item {
                ShimmerText(
                    "Ingresos vs Gastos",
                    baseColor = colors.textPrimary,
                    accentColor = colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
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

    if (showAiSummarySheet) {
        ModalBottomSheet(onDismissRequest = { showAiSummarySheet = false }) {
            MonthlySummarySection(
                state = summaryState,
                onGenerate = { summaryViewModel.generateSummary() },
                onRegenerate = { summaryViewModel.generateSummary(force = true) },
                onShare = onShareText,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun WeeklyAnomalyCard(anomalies: List<AnomalyAlert>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .glassCard()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔎", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Resumen semanal de gastos inusuales",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserrat
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Detectamos ${anomalies.size} gasto${if (anomalies.size == 1) "" else "s"} " +
                "por encima de tu patrón esta semana:",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = montserrat
        )
        Spacer(Modifier.height(8.dp))
        anomalies.take(5).forEach { a ->
            Text(
                text = "• ${a.category} — ${a.message}",
                color = colors.textPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = montserrat,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MonthlySummarySection(
    state: MonthlySummaryState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    var collapsed by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (!state.hasRequested) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenDark)
                ) {
                    Text(
                        "✨  Resumen IA del mes",
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
                .glassCard()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { collapsed = !collapsed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Resumen IA del mes",
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
                            text = "Generando tu resumen…",
                            baseColor = colors.textSecondary,
                            accentColor = colors.primary,
                            fontSize = 13.sp,
                            fontFamily = montserrat
                        )
                    }
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

                else -> {
                    Text(
                        state.summary,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = montserrat
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onShare(state.summary) }) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = FinTrackColors.GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir", color = FinTrackColors.GreenPrimary, fontFamily = montserrat)
                        }
                        TextButton(onClick = onRegenerate) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Regenerar", color = colors.textSecondary, fontFamily = montserrat)
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
    fotoUrl: String? = null,
    notificationCount: Int,
    onBellClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onAiSummaryClick: () -> Unit = {}
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hola,", color = colors.textSecondary, fontSize = 15.sp, fontFamily = montserrat)
            Spacer(Modifier.width(5.dp))
            ShimmerText(
                userName, baseColor = colors.textPrimary, accentColor = colors.primary,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat
            )
            Spacer(Modifier.width(6.dp))
            Text("👋", fontSize = 18.sp)
        }
        // Los 3 botones de accion (camara, IA, campana) van del mismo tamano entre si
        // (antes la campana era 40dp y los otros dos 34dp, se notaba disparejo) y con
        // un anillo animado (mismo shimmer que el borde de Centro Financiero) para
        // que tengan la misma "vida" que el resto de la app. El avatar se mantiene
        // mas grande que los 3: es la jerarquia visual (el usuario, no una accion).
        val actionButtonSize = 36.dp
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(actionButtonSize)
                    .background(colors.surfaceSecondary, CircleShape)
                    .border(1.dp, shimmerBorderBrush(baseColor = colors.border, accentColor = colors.primary), CircleShape)
                    .clickable(onClick = onCameraClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Escanear recibo", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(actionButtonSize)
                    .background(colors.surfaceSecondary, CircleShape)
                    .border(1.dp, shimmerBorderBrush(baseColor = colors.border, accentColor = colors.primary), CircleShape)
                    .clickable(onClick = onAiSummaryClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Resumen IA del mes", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
            }
            BadgedBox(badge = {
                if (notificationCount > 0) Badge(containerColor = FinTrackColors.ErrorColor) {
                    Text(if (notificationCount > 9) "9+" else "$notificationCount", fontSize = 9.sp, color = Color.White)
                }
            }) {
                Box(
                    modifier = Modifier
                        .size(actionButtonSize)
                        .background(colors.surfaceSecondary, CircleShape)
                        .border(1.dp, shimmerBorderBrush(baseColor = colors.border, accentColor = colors.primary), CircleShape)
                        .clickable(onClick = onBellClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)),
                        CircleShape
                    )
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (fotoUrl != null) {
                    AsyncImage(
                        model = fotoUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = montserratFamily()
                    )
                }
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
    // Los dos circulos decorativos ya existian (fijos); ahora ondean lento, como el
    // fondo del tema oscuro. Es el "movimiento" pedido sin mover la tarjeta ni su
    // contenido de lugar — solo la textura de fondo respira.
    val infiniteTransition = rememberInfiniteTransition(label = "balanceCardDrift")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 14_000, easing = LinearEasing)),
        label = "balanceCardPhase"
    )
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(FinTrackColors.GradientBalance)
        ) {
            Box(
                Modifier.size(200.dp)
                    .offset(x = 140.dp + 14.dp * sin(phase), y = -60.dp + 10.dp * cos(phase))
                    .background(Color.White.copy(alpha = 0.04f), CircleShape)
            )
            Box(
                Modifier.size(140.dp)
                    .offset(x = 160.dp + 10.dp * sin(phase + kotlin.math.PI.toFloat()), y = 60.dp + 14.dp * cos(phase + kotlin.math.PI.toFloat() / 2f))
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
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = FinTrackColors.GreenLight
                    )
                    KpiPill(
                        label = "Gastos",
                        value = if (saldoVisible) formatColonesCompacto(gastos) else "₡•••",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
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


/* Gráfica */

@Composable
private fun ChartSection(data: List<MonthlyChartData>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Últimos 6 meses", color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
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
        data.forEachIndexed { index, item ->
            // Stagger por mes: cada columna arranca un poco despues que la anterior,
            // para que el crecimiento se lea de izquierda a derecha en vez de todas
            // las barras subiendo a la vez.
            val barDelay = index * 70
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(maxH)
                ) {
                    GradientBar(fraction = item.ingresos / maxVal, width = 11.dp, brush = FinTrackColors.GradientGreenV, delayMillis = barDelay)
                    GradientBar(fraction = item.gastos / maxVal, width = 11.dp, brush = FinTrackColors.GradientRedV, delayMillis = barDelay + 60)
                }
                Spacer(Modifier.height(6.dp))
                Text(item.mes, color = colors.textSecondary, fontSize = 10.sp, fontFamily = montserrat)
            }
        }
    }
}

@Composable
private fun GradientBar(fraction: Float, width: Dp, brush: Brush, delayMillis: Int = 0) {
    val targetFraction = fraction.coerceIn(0.03f, 1f)
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 650, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(animatedFraction.value.coerceIn(0.001f, 1f))
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
    // Color semantico oficial (no el color arbitrario de la categoria) para el
    // porcentaje y la barra: verde si va bien, ambar en alerta, rojo en critico.
    val pct = item.porcentaje / 100f
    val statusColor = when {
        pct >= 0.90f -> FinTrackColors.ErrorColor
        pct >= 0.8f  -> FinTrackColors.WarningColor
        else         -> FinTrackColors.GreenPrimary
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .glassCard()
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
                    color = statusColor, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, fontFamily = montserrat
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.porcentaje / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.7f), statusColor))
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
                .glassCard()
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
                                    FinTrackColors.GradientGreen,
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
            .glassCard()
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
            .glassCard()
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
                    .background(FinTrackColors.GradientGreen)
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
                imageVector = if (item.esIngreso) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
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
        ShimmerText(title, baseColor = colors.textPrimary, accentColor = colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = montserrat)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(actionText, color = FinTrackColors.GreenPrimary, fontSize = 12.sp, fontFamily = montserrat, fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = FinTrackColors.GreenPrimary, modifier = Modifier.size(16.dp))
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
            .glassCard()
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}
