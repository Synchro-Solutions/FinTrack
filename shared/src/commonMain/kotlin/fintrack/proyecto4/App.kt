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
import fintrack.proyecto4.theme.FinTrackTypography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.navigation.FinTrackBottomBar
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.NavController
import fintrack.proyecto4.navigation.NavHost
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.navigation.mainScreens
import fintrack.proyecto4.ocr.OcrAssistantViewModel
import fintrack.proyecto4.screens.AguinaldoCalculatorScreen
import fintrack.proyecto4.screens.CalculatorPlaceholderScreen
import fintrack.proyecto4.screens.CurrencyConverterScreen
import fintrack.proyecto4.screens.DashboardScreen
import fintrack.proyecto4.screens.FinancialCenterScreen
import fintrack.proyecto4.screens.LoginScreen
import fintrack.proyecto4.screens.MetasScreen
import fintrack.proyecto4.screens.MovimientosScreen
import fintrack.proyecto4.screens.NetSalaryCalculatorScreen
import fintrack.proyecto4.screens.OcrAssistantScreen
import fintrack.proyecto4.screens.OcrConfirmScreen
import fintrack.proyecto4.screens.PresupuestosScreen
import fintrack.proyecto4.screens.TransactionFormScreen
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.transaction.TransactionType

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
    ocrCameraContent: @Composable (onCaptured: (String) -> Unit, onCancel: () -> Unit) -> Unit =
        { _, onCancel -> OcrCameraUnavailablePlaceholder(onCancel) },
    onPickReceiptImage: (onPicked: (String?) -> Unit) -> Unit = { onPicked -> onPicked(null) },
    onRecognizeReceiptText: suspend (imagePath: String) -> String = { "" }
) {
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

        // Vive mientras dure la sesión de la app (igual que navController) para que el
        // estado del asistente sobreviva a la navegación intermedia hacia Screen.OcrCamera.
        // Se reinicia explícitamente (reset()) cada vez que se abre desde el botón de OCR.
        val ocrAssistantViewModel = remember {
            OcrAssistantViewModel(recognizeText = onRecognizeReceiptText)
        }

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
                            },
                            onOcrClick = {
                                ocrAssistantViewModel.reset()
                                navController.navigate(Screen.OcrAssistant)
                            }
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
                            onCancel = { navController.popToRoot() },
                            onSaved = { navController.replace(Screen.Movimientos) }
                        )

                        is Screen.Movimientos -> MovimientosScreen()
                        is Screen.Presupuestos -> PresupuestosScreen()
                        is Screen.Metas -> MetasScreen()
                        is Screen.Mas,
                        is Screen.FinancialCenter -> FinancialCenterScreen(historyCount = 0)
                        is Screen.AguinaldoCalculator -> AguinaldoCalculatorScreen(
                            onBack = { navController.goBack() }
                        )
                        is Screen.CurrencyConverter -> CurrencyConverterScreen(
                            onBack = { navController.goBack() }
                        )
                        is Screen.NetSalaryCalculator -> NetSalaryCalculatorScreen(
                            onBack = { navController.goBack() },
                            onSaved = {
                                // TODO: persistir el calculo (financial_calculation, calc_type=SALARIO_NETO)
                                // cuando exista un repositorio/historial real.
                                navController.goBack()
                            }
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

/**
 * Contenido por defecto de Screen.OcrCamera cuando la plataforma no inyectó una
 * implementación real (p.ej. Web/Desktop, donde CameraX/ML Kit no aplican: el
 * asistente OCR es una funcionalidad Android-specific para este sprint).
 */
@Composable
private fun OcrCameraUnavailablePlaceholder(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "La cámara no está disponible en esta plataforma.",
            color = FinTrackColors.TextPrimary
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCancel) {
            Text("Volver")
        }
    }
}
