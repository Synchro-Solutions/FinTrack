package fintrack.proyecto4.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Pantallas disponibles en la aplicación con sus respectivos argumentos tipados.
 */
sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data object InitialConfig : Screen
    data object Dashboard : Screen
}

/**
 * Dirección de la navegación para aplicar la animación adecuada.
 */
enum class NavDirection {
    PUSH, POP
}

/**
 * Mantiene la pila de pantallas (backstack) y la dirección de la última acción de navegación.
 */
class NavController(initialScreen: Screen = Screen.Login) {
    private val _backstack = mutableStateListOf<Screen>(initialScreen)
    
    val backstack: List<Screen> get() = _backstack

    var lastDirection by mutableStateOf(NavDirection.PUSH)
        private set

    val currentScreen: Screen
        get() = _backstack.lastOrNull() ?: Screen.Login

    val canGoBack: Boolean
        get() = _backstack.size > 1

    fun navigate(screen: Screen) {
        lastDirection = NavDirection.PUSH
        _backstack.add(screen)
    }

    fun goBack(): Boolean {
        if (canGoBack) {
            lastDirection = NavDirection.POP
            _backstack.removeAt(_backstack.lastIndex)
            return true
        }
        return false
    }

    fun replace(screen: Screen) {
        lastDirection = NavDirection.PUSH
        if (_backstack.isNotEmpty()) {
            _backstack.removeAt(_backstack.lastIndex)
        }
        _backstack.add(screen)
    }

    fun popToRoot() {
        if (_backstack.size > 1) {
            lastDirection = NavDirection.POP
            val first = _backstack.first()
            _backstack.clear()
            _backstack.add(first)
        }
    }
}

/**
 * Inyector de contexto para acceder al NavController desde cualquier Composable.
 */
val LocalNavController = staticCompositionLocalOf<NavController> {
    error("NavController no inicializado. Asegúrate de proveerlo usando CompositionLocalProvider.")
}

/**
 * Contenedor de navegación que reacciona a los cambios en el NavController actual.
 * Proporciona transiciones horizontales premium según la dirección (PUSH / POP).
 */
@Composable
fun NavHost(
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = navController.currentScreen,
        transitionSpec = {
            if (navController.lastDirection == NavDirection.PUSH) {
                // Entra desde la derecha, sale por la izquierda
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                // Entra desde la izquierda, sale por la derecha (retroceder)
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        modifier = modifier,
        label = "NavTransition"
    ) { screen ->
        content(screen)
    }
}
