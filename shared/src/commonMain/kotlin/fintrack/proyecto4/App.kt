package fintrack.proyecto4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import fintrack.proyecto4.theme.FinTrackTypography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.notifications.BudgetAlertService
import fintrack.proyecto4.notifications.NoOpNotificationRepository
import fintrack.proyecto4.notifications.NotificationRepository
import fintrack.proyecto4.navigation.FinTrackBottomBar
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.NavController
import fintrack.proyecto4.navigation.NavHost
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.navigation.mainScreens
import fintrack.proyecto4.ocr.OcrAssistantViewModel
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingViewModel
import fintrack.proyecto4.screens.AiChatScreen
import fintrack.proyecto4.screens.AguinaldoCalculatorScreen
import fintrack.proyecto4.screens.AjustesScreen
import fintrack.proyecto4.screens.CalculatorPlaceholderScreen
import fintrack.proyecto4.screens.CurrencyConverterScreen
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.FinancialCenterScreen
import fintrack.proyecto4.screens.LoginScreen
import fintrack.proyecto4.screens.MasScreen
import fintrack.proyecto4.screens.MetasScreen
import fintrack.proyecto4.screens.TransactionsScreen
import fintrack.proyecto4.screens.NetSalaryCalculatorScreen
import fintrack.proyecto4.screens.OcrAssistantScreen
import fintrack.proyecto4.screens.NotificationsScreen
import fintrack.proyecto4.screens.OcrConfirmScreen
import fintrack.proyecto4.screens.CreateBudgetScreen
import fintrack.proyecto4.screens.OnboardingScreen
import fintrack.proyecto4.screens.PresupuestosScreen
import fintrack.proyecto4.screens.TransactionDetailScreen
import fintrack.proyecto4.screens.TransactionFormScreen
import fintrack.proyecto4.theme.DarkAppColors
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LightAppColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.launch

private val DarkColorScheme = darkColorScheme(
    primary      = FinTrackColors.GreenPrimary,
    background   = DarkAppColors.bg,
    surface      = DarkAppColors.surface,
    onBackground = DarkAppColors.textPrimary,
    onSurface    = DarkAppColors.textPrimary
)

private val LightColorScheme = lightColorScheme(
    primary      = FinTrackColors.GreenPrimary,
    background   = LightAppColors.bg,
    surface      = LightAppColors.surface,
    onBackground = LightAppColors.textPrimary,
    onSurface    = LightAppColors.textPrimary
)

/**
 * @param ocrCameraContent Contenido de la pantalla de captura en vivo (CameraX en Android).
 *   Recibe un callback con la ruta del archivo capturado y otro para cancelar.
 *   Se inyecta desde el entry point de cada plataforma (ver androidApp/MainActivity.kt);
 *   por defecto muestra un placeholder para plataformas donde la cámara no está integrada.
 * @param onPickReceiptImage Lanza el selector de imágenes de la plataforma; invoca el callback
 *   recibido con la ruta del archivo elegido, o null si el usuario canceló.
 * @param onRecognizeReceiptText Ejecuta OCR (ML Kit en Android) sobre la imagen indicada y
 *   retorna el texto plano detectado.
 */
@Composable
fun App(
    authRepository: AuthRepository,
    onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    notificationRepository: NotificationRepository = NoOpNotificationRepository(),
    ocrCameraContent: @Composable (onCaptured: (String) -> Unit, onCancel: () -> Unit) -> Unit =
        { _, onCancel -> OcrCameraUnavailablePlaceholder(onCancel) },
    onPickReceiptImage: (onPicked: (String?) -> Unit) -> Unit = { onPicked -> onPicked(null) },
    onPickProfilePhoto: (onPicked: (String?) -> Unit) -> Unit = { onPicked -> onPicked(null) },
    onRecognizeReceiptText: suspend (imagePath: String) -> String = { "" }
) {
    var initialScreen by remember { mutableStateOf<Screen?>(null) }
    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = authRepository.getStoredToken()
        if (token != null) {
            val uid = AuthClient.currentUserId()
            val done = if (uid != null) onboardingRepository.isOnboardingComplete(uid) else true
            initialScreen = if (done) Screen.Dashboard else Screen.Onboarding
        } else {
            initialScreen = Screen.Login
        }
    }

    val appColors = if (isDarkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = FinTrackTypography()) {
        CompositionLocalProvider(LocalAppColors provides appColors) {
            if (initialScreen == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appColors.bg)
                )
                return@CompositionLocalProvider
            }

            val navController = remember(initialScreen) { NavController(initialScreen!!) }
            val currentScreen by remember { derivedStateOf { navController.currentScreen } }
            val showBottomBar = currentScreen in mainScreens
            val scope = rememberCoroutineScope()

            val ocrAssistantViewModel = remember {
                OcrAssistantViewModel(recognizeText = onRecognizeReceiptText)
            }

            val budgetAlertService = remember {
                BudgetAlertService(budgetRepository, notificationRepository, onboardingRepository)
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                Scaffold(
                    containerColor = appColors.bg,
                    bottomBar = {
                        FinTrackBottomBar(
                            currentScreen = currentScreen,
                            visible = showBottomBar,
                            onTabSelected = { screen ->
                                if (screen != currentScreen) navController.replace(screen)
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            is Screen.Login -> LoginScreen(
                                authRepository = authRepository,
                                onLoginSuccess = {
                                    scope.launch {
                                        val uid = AuthClient.currentUserId()
                                        val done = if (uid != null) {
                                            onboardingRepository.isOnboardingComplete(uid)
                                        } else true
                                        if (done) navController.replace(Screen.Dashboard)
                                        else navController.replace(Screen.Onboarding)
                                    }
                                }
                            )

                            is Screen.Onboarding -> {
                                val uid = AuthClient.currentUserId() ?: ""
                                val onboardingVm = remember(uid) {
                                    OnboardingViewModel(
                                        repository = onboardingRepository,
                                        uid = uid
                                    )
                                }
                                OnboardingScreen(
                                    viewModel = onboardingVm,
                                    onFinished = { navController.replace(Screen.Dashboard) },
                                    onPickPhoto = onPickProfilePhoto
                                )
                            }

                            is Screen.Dashboard -> DashboardScreen(
                                transactionRepository = transactionRepository,
                                onboardingRepository = onboardingRepository,
                                budgetRepository = budgetRepository,
                                notificationRepository = notificationRepository,
                                onNavigateToNotifications = { navController.navigate(Screen.Notifications) },
                                onNavigateToIngreso = {
                                    navController.navigate(Screen.TransactionForm(TransactionType.INCOME))
                                },
                                onNavigateToGasto = {
                                    navController.navigate(Screen.TransactionForm(TransactionType.EXPENSE))
                                },
                                onNavigateToAjustes = { navController.navigate(Screen.Ajustes) },
                                onNavigateToMovimientos = { navController.replace(Screen.Movimientos) },
                                onNavigateToPresupuestos = { navController.replace(Screen.Presupuestos) },
                                onNavigateToMetas = { navController.replace(Screen.Metas) },
                                onNavigateToChat = { navController.navigate(Screen.AiChat) }
                            )

                        is Screen.TransactionForm -> TransactionFormScreen(
                            initialType = screen.initialType,
                            editingTransaction = screen.editingTransaction,
                            transactionRepository = transactionRepository,
                            budgetAlertService = budgetAlertService,
                            onBack = {
                                navController.goBack()
                            },
                            onSaved = {
                                navController.replace(Screen.Movimientos)
                            },
                            onOcrClick = {
                                ocrAssistantViewModel.reset()
                                navController.navigate(Screen.OcrAssistant)
                            }
                        )

                        is Screen.TransactionDetail -> TransactionDetailScreen(
                            transaction = screen.transaction,
                            transactionRepository = transactionRepository,
                            onBack = { navController.goBack() },
                            onEdit = { transaction ->
                                navController.navigate(
                                    Screen.TransactionForm(transaction.type, transaction)
                                )
                            },
                            onDeleted = { navController.goBack() }
                        )

                        is Screen.OcrAssistant -> OcrAssistantScreen(
                            viewModel = ocrAssistantViewModel,
                            onBack = { navController.goBack() },
                            onTakePhotoClick = { navController.navigate(Screen.OcrCamera) },
                            onPickImageClick = {
                                onPickReceiptImage { path ->
                                    if (path != null) ocrAssistantViewModel.processImage(path)
                                }
                            },
                            onReviewData = { result ->
                                navController.navigate(Screen.OcrConfirm(result))
                            }
                        )

                        is Screen.OcrCamera -> ocrCameraContent(
                            { path ->
                                ocrAssistantViewModel.processImage(path)
                                navController.goBack()
                            },
                            { navController.goBack() }
                        )

                        is Screen.OcrConfirm -> OcrConfirmScreen(
                            result = screen.result,
                            transactionRepository = transactionRepository,
                            budgetAlertService = budgetAlertService,
                            onCancel = {
                                // Screen.OcrAssistant solo se alcanza desde el formulario manual
                                // (ver onOcrClick más arriba), así que siempre queda justo debajo
                                // en el backstack: dos goBack() cancelan todo el flujo OCR y
                                // regresan al formulario manual donde el usuario estaba.
                                navController.goBack()
                                navController.goBack()
                            },
                            onSaved = { navController.replace(Screen.Movimientos) }
                        )

                        is Screen.Movimientos -> TransactionsScreen(
                            transactionRepository = transactionRepository,
                            onAddClick = {
                                navController.navigate(Screen.TransactionForm(TransactionType.EXPENSE))
                            },
                            onTransactionClick = { transaction ->
                                navController.navigate(Screen.TransactionDetail(transaction))
                            }
                        )
                        is Screen.Presupuestos -> PresupuestosScreen(
                            budgetRepository = budgetRepository,
                            transactionRepository = transactionRepository,
                            onNuevoPresupuesto = { navController.navigate(Screen.NuevoPresupuesto) },
                            onTransactionClick = { transaction ->
                                navController.navigate(Screen.TransactionDetail(transaction))
                            }
                        )
                        is Screen.NuevoPresupuesto -> CreateBudgetScreen(
                            budgetRepository = budgetRepository,
                            onBack = { navController.goBack() },
                            onSaved = { navController.replace(Screen.Presupuestos) }
                        )
                        is Screen.Metas -> MetasScreen()

                        is Screen.AiChat -> AiChatScreen(
                            transactionRepository = transactionRepository,
                            onBack = { navController.goBack() }
                        )

                        is Screen.Notifications -> NotificationsScreen(
                            notificationRepository = notificationRepository,
                            onBack = { navController.goBack() }
                        )

                        is Screen.Mas -> MasScreen(
                            onNavigateToChat = { navController.navigate(Screen.AiChat) }
                        )
                        is Screen.Ajustes -> AjustesScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onboardingRepository = onboardingRepository,
                            onBack = { navController.goBack() },
                            onCerrarSesion = { navController.replace(Screen.Login) }
                        )

                        is Screen.FinancialCenter -> FinancialCenterScreen(historyCount = 0)
                        is Screen.AguinaldoCalculator -> AguinaldoCalculatorScreen(
                            onBack = { navController.goBack() }
                        )
                        is Screen.CurrencyConverter -> CurrencyConverterScreen(
                            onBack = { navController.goBack() }
                        )
                        is Screen.NetSalaryCalculator -> NetSalaryCalculatorScreen(
                            onBack = { navController.goBack() },
                            onSaved = { navController.goBack() }
                        )
                        is Screen.LiquidacionCalculator -> CalculatorPlaceholderScreen(
                            title = "Liquidacion",
                            description = "Aqui va la calculadora de liquidacion."
                        )
                        is Screen.CesantiaCalculator -> CalculatorPlaceholderScreen(
                            title = "Cesantia",
                            description = "Aqui va la calculadora de cesantia."
                        )
                        is Screen.VacacionesCalculator -> CalculatorPlaceholderScreen(
                            title = "Vacaciones",
                            description = "Aqui va la calculadora de vacaciones."
                        )
                        is Screen.PreavisoCalculator -> CalculatorPlaceholderScreen(
                            title = "Preaviso",
                            description = "Aqui va la calculadora de preaviso."
                        )
                        is Screen.CalculationHistory -> CalculatorPlaceholderScreen(
                            title = "Historial",
                            description = "Aqui va el historial de calculos guardados."
                        )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Contenido por defecto de Screen.OcrCamera cuando la plataforma no inyectó una
 * implementación real (p.ej. Web/Desktop, donde CameraX/ML Kit no aplican: el
 * asistente OCR es una funcionalidad Android-specific para este sprint).
 */
@Composable
private fun OcrCameraUnavailablePlaceholder(onCancel: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "La cámara no está disponible en esta plataforma.",
            color = colors.textPrimary
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCancel) {
            Text("Volver")
        }
    }
}
