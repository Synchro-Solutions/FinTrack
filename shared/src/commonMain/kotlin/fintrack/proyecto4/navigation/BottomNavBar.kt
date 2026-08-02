package fintrack.proyecto4.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

private val navItems = listOf(
    NavItem("Inicio", Icons.Default.Home, Screen.Dashboard),
    NavItem("Movimientos", Icons.Default.SwapHoriz, Screen.Movimientos),
    NavItem("Asistente IA", Icons.Default.AutoAwesome, Screen.AiChat),
    NavItem("Presupuestos", Icons.Default.AccountBalance, Screen.Presupuestos),
    NavItem("Más", Icons.Default.MoreHoriz, Screen.FinancialCenter)
)

@Composable
fun FinTrackBottomBar(
    currentScreen: Screen,
    visible: Boolean,
    notificationCount: Int = 0,
    ocrPendingCount: Int = 0,
    onTabSelected: (Screen) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(NavTransitionDurationMillis, easing = NavTransitionEasing)
        ) { it } + fadeIn(tween(NavTransitionDurationMillis, easing = NavTransitionEasing)),
        exit = slideOutVertically(
            animationSpec = tween(NavTransitionDurationMillis, easing = NavTransitionEasing)
        ) { it } + fadeOut(tween(NavTransitionDurationMillis, easing = NavTransitionEasing))
    ) {
        val colors = LocalAppColors.current
        // En claro, el fondo decorativo del app shell (FinTrackAppBackground) ya llega
        // saturado en primary a esta altura de la pantalla, así que el nav queda
        // transparente para fundirse con ese verde en vez de tapar con su propia
        // superficie clara. En oscuro el glow es solo un halo arriba, no llega abajo,
        // así que ahí sigue con su superficie opaca normal.
        val navContainerColor = if (colors.isDark) colors.navBar else Color.Transparent
        val navContentColor = if (colors.isDark) colors.textSecondary else Color.White.copy(alpha = 0.7f)
        val navSelectedColor = if (colors.isDark) FinTrackColors.GreenPrimary else Color.White
        NavigationBar(
            containerColor = navContainerColor,
            tonalElevation = 0.dp
        ) {
            navItems.forEach { item ->
                val selected = currentScreen == item.screen
                val badgeCount = when (item.screen) {
                    Screen.Dashboard -> notificationCount
                    Screen.Movimientos -> ocrPendingCount
                    else -> 0
                }

                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(item.screen) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = FinTrackColors.ErrorColor
                                    ) {
                                        Text(
                                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                            color = FinTrackColors.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navSelectedColor,
                        selectedTextColor = navSelectedColor,
                        unselectedIconColor = navContentColor,
                        unselectedTextColor = navContentColor,
                        indicatorColor = navSelectedColor.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}
