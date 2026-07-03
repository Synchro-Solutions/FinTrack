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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
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

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

private val navItems = listOf(
    NavItem("Inicio", Icons.Default.Home, Screen.Dashboard),
    NavItem("Movimientos", Icons.Default.SwapHoriz, Screen.Movimientos),
    NavItem("Presupuestos", Icons.Default.AccountBalance, Screen.Presupuestos),
    NavItem("Metas", Icons.Default.Star, Screen.Metas),
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
        NavigationBar(
            containerColor = Color(0xFF0F1923),
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
                        selectedIconColor = FinTrackColors.GreenPrimary,
                        selectedTextColor = FinTrackColors.GreenPrimary,
                        unselectedIconColor = FinTrackColors.WhiteAlpha70,
                        unselectedTextColor = FinTrackColors.WhiteAlpha70,
                        indicatorColor = FinTrackColors.GreenPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}
