package fintrack.proyecto4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import fintrack.proyecto4.theme.FinTrackTypography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.navigation.FinTrackBottomBar
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.NavController
import fintrack.proyecto4.navigation.NavHost
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.navigation.mainScreens
import fintrack.proyecto4.screens.AjustesScreen
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.LoginScreen
import fintrack.proyecto4.screens.MasScreen
import fintrack.proyecto4.screens.MetasScreen
import fintrack.proyecto4.screens.MovimientosScreen
import fintrack.proyecto4.screens.PresupuestosScreen
import fintrack.proyecto4.screens.TransactionFormScreen
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.transaction.TransactionType

private val DarkColorScheme = darkColorScheme(
    primary          = FinTrackColors.GreenPrimary,
    background       = Color(0xFF080E1A),
    surface          = Color(0xFF111827),
    onBackground     = Color(0xFFF1F5F9),
    onSurface        = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
    primary          = FinTrackColors.GreenPrimary,
    background       = Color(0xFFF1F5F9),
    surface          = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF0F172A),
    onSurface        = Color(0xFF0F172A)
)

@Composable
fun App(authRepository: AuthRepository) {
    var initialScreen by remember { mutableStateOf<Screen?>(null) }
    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = authRepository.getStoredToken()
        initialScreen = if (token != null) Screen.Dashboard else Screen.Login
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val bgColor = colorScheme.background

    MaterialTheme(colorScheme = colorScheme, typography = FinTrackTypography()) {
        if (initialScreen == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            )
            return@MaterialTheme
        }

        val navController = remember(initialScreen) { NavController(initialScreen!!) }
        val currentScreen by remember { derivedStateOf { navController.currentScreen } }
        val showBottomBar = currentScreen in mainScreens

        CompositionLocalProvider(LocalNavController provides navController) {
            Scaffold(
                containerColor = Color(0xFF0F172A),
                bottomBar = {
                    FinTrackBottomBar(
                        currentScreen = currentScreen,
                        visible = showBottomBar,
                        onTabSelected = { screen ->
                            if (screen != currentScreen) {
                                navController.replace(screen)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                ) { screen ->
                    when (screen) {
                        is Screen.Login -> LoginScreen(authRepository)

                        is Screen.Dashboard -> DashboardScreen(
                            onNavigateToIngreso = {
                                navController.navigate(Screen.TransactionForm(TransactionType.INCOME))
                            },
                            onNavigateToGasto = {
                                navController.navigate(Screen.TransactionForm(TransactionType.EXPENSE))
                            },
                            onNavigateToAjustes = { navController.navigate(Screen.Ajustes) }
                        )

                        is Screen.TransactionForm -> TransactionFormScreen(
                            initialType = screen.initialType,
                            onBack = { navController.goBack() },
                            onSaved = { navController.replace(Screen.Movimientos) }
                        )

                        is Screen.Movimientos -> MovimientosScreen()
                        is Screen.Presupuestos -> PresupuestosScreen()
                        is Screen.Metas -> MetasScreen()
                        is Screen.Mas -> MasScreen()
                        is Screen.Ajustes -> AjustesScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onCerrarSesion = { navController.replace(Screen.Login) }
                        )
                    }
                }
            }
        }
    }
}
