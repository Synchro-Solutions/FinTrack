package fintrack.proyecto4.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import fintrack.proyecto4.ocr.OcrResult
import fintrack.proyecto4.transaction.Transaction
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

    /** Registro de cuenta nueva con email y contraseña (US-01). */
    data object Register : Screen

    data object ForgotPassword : Screen
    data object Onboarding : Screen
    data object Dashboard : Screen
    data object Movimientos : Screen
    data object Presupuestos : Screen
    data object Metas : Screen
    data object Mas : Screen
    data object Ajustes : Screen

    /** Bandeja de notificaciones (US-43). */
    data object Notifications : Screen

    /** Editar nombre, foto, ingreso y moneda del perfil (US-09). */
    data object EditarPerfil : Screen

    data class TransactionForm(
        val initialType: TransactionType,
        /** Si no es null, el formulario edita esta transacción en vez de crear una nueva (US-14). */
        val editingTransaction: Transaction? = null
    ) : Screen

    /** Ver/editar/eliminar el detalle de una transacción ya guardada (US-14). */
    data class TransactionDetail(
        val transaction: Transaction
    ) : Screen

    /** Pantalla del asistente OCR (idle / procesando / éxito, ver UI-05). */
    data object OcrAssistant : Screen

    /** Captura en vivo con CameraX (contenido inyectado por la plataforma, ver INT-03). */
    data object OcrCamera : Screen

    /** Revisión/confirmación de los datos detectados antes de guardar la transacción. */
    data class OcrConfirm(
        val result: OcrResult
    ) : Screen

    data object AiChat : Screen
    data object NuevoPresupuesto : Screen
    data object FinancialCenter : Screen
    data object Reportes : Screen
    data object AguinaldoCalculator : Screen
    data object CurrencyConverter : Screen
    data object NetSalaryCalculator : Screen
    data object LiquidacionCalculator : Screen
    data object CesantiaCalculator : Screen
    data object VacacionesCalculator : Screen
    data object PreavisoCalculator : Screen
    data object CalculationHistory : Screen
}

val mainScreens = setOf(
    Screen.Dashboard,
    Screen.Movimientos,
    Screen.AiChat,
    Screen.Presupuestos,
    Screen.FinancialCenter
)

/**
 * Dirección de la navegación para aplicar la animación adecuada.
 */
enum class NavDirection {
    PUSH, POP
}

/**
 * Una pantalla dentro del backstack, con un [id] unico por cada vez que se empujo (no por
 * tipo de pantalla): dos visitas a la misma [Screen] (p.ej. entrar a Metas, volver, entrar
 * de nuevo) son dos entradas distintas. Ese id es lo que usa [NavHost] para darle a cada
 * visita su propio [androidx.lifecycle.ViewModelStore] en vez de reusar uno compartido.
 */
data class BackStackEntry(val id: Long, val screen: Screen)

/**
 * Mantiene la pila de pantallas (backstack) y la dirección de la última acción de navegación.
 */
class NavController(initialScreen: Screen = Screen.Login) {
    private var nextEntryId = 0L
    private fun newEntry(screen: Screen) = BackStackEntry(nextEntryId++, screen)

    private val _backstack = mutableStateListOf(newEntry(initialScreen))

    val backstack: List<Screen> get() = _backstack.map { it.screen }

    var lastDirection by mutableStateOf(NavDirection.PUSH)
        private set

    val currentEntry: BackStackEntry
        get() = _backstack.last()

    val currentScreen: Screen
        get() = currentEntry.screen

    val canGoBack: Boolean
        get() = _backstack.size > 1

    fun navigate(screen: Screen) {
        lastDirection = NavDirection.PUSH
        _backstack.add(newEntry(screen))
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
        _backstack.add(newEntry(screen))
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
 *
 * Cada entrada del backstack recibe su propio [ViewModelStoreOwner]: sin esto, todas las
 * pantallas comparten el ViewModelStore de la Activity (via LocalViewModelStoreOwner por
 * defecto) y como este NavHost es propio, no Jetpack Navigation Compose, ningun
 * `viewModel(key = ...)` se limpiaba nunca al salir de una pantalla. Recorrer muchas
 * pantallas en una sesion iba dejando ViewModels y sus listeners de Firestore acumulados
 * y vivos indefinidamente, degradando la app hasta sentirse "trabada" en la pantalla que
 * tocara visitar despues. Ahora el store de cada entrada se limpia (`clear()`, que a su vez
 * llama `onCleared()` en cada ViewModel) cuando Compose la descompone definitivamente tras
 * un pop/replace — el `DisposableEffect` esta atado a la composicion real, no a cuando se
 * remueve del backstack, asi que no corta nada a mitad de la animacion de salida.
 */
@Composable
fun NavHost(
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = navController.currentEntry,
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
    ) { entry ->
        val viewModelStoreOwner = remember(entry.id) {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(entry.id) {
            onDispose { viewModelStoreOwner.viewModelStore.clear() }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            content(entry.screen)
        }
    }
}
