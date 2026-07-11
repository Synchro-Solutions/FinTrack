package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily

internal data class FinancialMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBrush: Brush,
    val route: Screen
)

internal fun financialMenuItems(): List<FinancialMenuItem> = listOf(
    FinancialMenuItem("Aguinaldo", "Calcula tu aguinaldo estimado", Icons.Default.Star, FinTrackColors.GradientViolet, Screen.AguinaldoCalculator),
    FinancialMenuItem("Conversor", "CRC, USD, EUR y mas", Icons.Default.SwapHoriz, FinTrackColors.GradientIndigo, Screen.CurrencyConverter),
    FinancialMenuItem("Salario neto", "Rebajas CCSS y renta", Icons.AutoMirrored.Filled.TrendingUp, FinTrackColors.GradientGreen, Screen.NetSalaryCalculator),
    FinancialMenuItem("Liquidacion", "Estimado laboral al cesar", Icons.AutoMirrored.Filled.TrendingDown, FinTrackColors.GradientRed, Screen.LiquidacionCalculator),
    FinancialMenuItem("Cesantia", "Auxilio de cesantia CR", Icons.Default.Home, FinTrackColors.GradientGreen, Screen.CesantiaCalculator),
    FinancialMenuItem("Vacaciones", "Dias pendientes de pago", Icons.Default.CalendarToday, FinTrackColors.GradientViolet, Screen.VacacionesCalculator),
    FinancialMenuItem("Preaviso", "Calculo de preaviso laboral", Icons.Default.Notifications, FinTrackColors.GradientRed, Screen.PreavisoCalculator),
    FinancialMenuItem("Historial", "Calculos guardados", Icons.Default.MoreHoriz, FinTrackColors.GradientIndigo, Screen.CalculationHistory)
)

@Composable
fun FinancialCenterScreen(historyCount: Int = 0) {
    val navController = LocalNavController.current
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current

    val menuItems = financialMenuItems()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 12.dp else 16.dp
        val cardMinHeight = if (compact) 132.dp else 144.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (compact) 16.dp else 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(FinTrackColors.GradientBalance)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Centro financiero",
                            fontSize = if (compact) 23.sp else 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = montserrat
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { navController.replace(Screen.Dashboard) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Ir al inicio",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Inicio",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = montserrat
                            )
                        }
                    }
                    Text(
                        text = "Calculadoras y herramientas para tus decisiones",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontFamily = montserrat,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    FinancialCard(
                        item = item,
                        minHeight = cardMinHeight,
                        badgeCount = if (item.route == Screen.CalculationHistory) historyCount else 0,
                        onClick = { navController.navigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialCard(
    item: FinancialMenuItem,
    minHeight: Dp,
    badgeCount: Int,
    onClick: () -> Unit
) {
    val montserrat = montserratFamily()
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.iconBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (badgeCount > 0) {
                    Badge(
                        containerColor = FinTrackColors.GreenPrimary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = montserrat
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = item.title,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                fontFamily = montserrat,
                modifier = Modifier.padding(top = 10.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = montserrat,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

