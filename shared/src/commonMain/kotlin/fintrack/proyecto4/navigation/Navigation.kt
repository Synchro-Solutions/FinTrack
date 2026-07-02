package fintrack.proyecto4.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import fintrack.proyecto4.ocr.OcrResult
import fintrack.proyecto4.transaction.TransactionType

/**
 * Especificación de animación única compartida entre la transición de pantallas (NavHost)
 * y la aparición/desaparición de la barra inferior (FinTrackBottomBar), para que ambas
 * inicien y terminen exactamente al mismo tiempo y la transición se sienta uniforme.
 */
internal const val NavTransitionDurationMillis = 320
internal val NavTransitionEasing = FastOutSlowInEasing

/**
 * Pantallas disponibles en la aplicación con sus respectivos argumentos tipados.
 */
sealed interface Screen {
    data object Login : Screen
    data object Onboarding : Screen
    data object Dashboard : Screen
    data object Movimientos : Screen
    data object Presupuestos : Screen
    data object Metas : Screen
    data object Mas : Screen

    data class TransactionForm(
        val initialType: TransactionType
    ) : Screen

    /** Pantalla del asistente OCR (idle / procesando / éxito, ver UI-05). */
    data object OcrAssistant : Screen

    /** Captura en vivo con CameraX (contenido inyectado por la plataforma, ver INT-03). */
    data object OcrCamera : Screen

    /** Revisión/confirmación de los datos detectados antes de guardar la transacción. */
    data class OcrConfirm(
        val result: OcrResult
    ) : Screen
    
    data object FinancialCenter : Screen
    data object AguinaldoCalculator : Screen
    data object CurrencyConverter : Screen
    data object NetSalaryCalculator : Screen
    data object Rule503020Calculator : Screen
    data object LiquidacionCalculator : Screen
    data object CesantiaCalculator : Screen
    data object VacacionesCalculator : Screen
    data object PreavisoCalculator : Screen
    data object CalculationHistory : Screen
}

val mainScreens = setOf(
    Screen.Dashboard,
    Screen.Movimientos,
    Screen.Presupuestos,
    Screen.Metas,
    Screen.Mas
)

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
            val offsetSpec = tween<IntOffset>(NavTransitionDurationMillis, easing = NavTransitionEasing)
            val fadeSpec = tween<Float>(NavTransitionDurationMillis, easing = NavTransitionEasing)

            if (navController.lastDirection == NavDirection.PUSH) {
                // Entra desde la derecha, sale por la izquierda
                (slideInHorizontally(offsetSpec) { width -> width } + fadeIn(fadeSpec)).togetherWith(
                    slideOutHorizontally(offsetSpec) { width -> -width } + fadeOut(fadeSpec)
                )
            } else {
                // Entra desde la izquierda, sale por la derecha (retroceder)
                (slideInHorizontally(offsetSpec) { width -> -width } + fadeIn(fadeSpec)).togetherWith(
                    slideOutHorizontally(offsetSpec) { width -> width } + fadeOut(fadeSpec)
                )
            }
        },
        modifier = modifier,
        label = "NavTransition"
    ) { screen ->
        content(screen)
    }
}
