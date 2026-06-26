package fintrack.proyecto4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import fintrack.proyecto4.theme.FinTrackTypography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.NavController
import fintrack.proyecto4.navigation.NavHost
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.LoginScreen

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

        CompositionLocalProvider(LocalNavController provides navController) {
            NavHost(navController = navController) { screen ->
                when (screen) {
                    is Screen.Login -> LoginScreen(authRepository)
                    is Screen.Dashboard -> DashboardScreen()
                }
            }
        }
    }
}
