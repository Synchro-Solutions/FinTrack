package fintrack.proyecto4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.LoginScreen
import fintrack.proyecto4.screens.MasScreen
import fintrack.proyecto4.screens.MetasScreen
import fintrack.proyecto4.screens.MovimientosScreen
import fintrack.proyecto4.screens.PresupuestosScreen
import fintrack.proyecto4.screens.TransactionFormScreen
import fintrack.proyecto4.transaction.TransactionType

@Composable
fun App(authRepository: AuthRepository) {
    var initialScreen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(Unit) {
        val token = authRepository.getStoredToken()
        initialScreen = if (token != null) Screen.Dashboard else Screen.Login
    }

    MaterialTheme(typography = FinTrackTypography()) {
        if (initialScreen == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
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
                                navController.navigate(
                                    Screen.TransactionForm(TransactionType.INCOME)
                                )
                            },
                            onNavigateToGasto = {
                                navController.navigate(
                                    Screen.TransactionForm(TransactionType.EXPENSE)
                                )
                            }
                        )

                        is Screen.TransactionForm -> TransactionFormScreen(
                            initialType = screen.initialType,
                            onBack = {
                                navController.goBack()
                            },
                            onSaved = {
                                navController.replace(Screen.Movimientos)
                            }
                        )

                        is Screen.Movimientos -> MovimientosScreen()
                        is Screen.Presupuestos -> PresupuestosScreen()
                        is Screen.Metas -> MetasScreen()
                        is Screen.Mas -> MasScreen()
                    }
                }
            }
        }
    }
}
