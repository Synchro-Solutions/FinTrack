package fintrack.proyecto4

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.NavController
import fintrack.proyecto4.navigation.NavHost
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.LoginScreen

@Composable
@Preview
fun App() {
    val navController = remember { NavController(Screen.Login) }

    MaterialTheme {
        CompositionLocalProvider(LocalNavController provides navController) {
            NavHost(navController = navController) { screen ->
                when (screen) {
                    is Screen.Login -> LoginScreen()
                    is Screen.Dashboard -> DashboardScreen()
                }
            }
        }
    }
}
