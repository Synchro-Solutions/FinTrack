package fintrack.proyecto4

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.preferencesDataStore
import fintrack.proyecto4.auth.DataStoreSessionStore
import fintrack.proyecto4.auth.FirebaseAuthRepository
import fintrack.proyecto4.budget.FirestoreBudgetRepository
import fintrack.proyecto4.notifications.AndroidNotifierContext
import fintrack.proyecto4.notifications.FirestoreNotificationRepository
import fintrack.proyecto4.ocr.CameraXCaptureScreen
import fintrack.proyecto4.ocr.recognizeReceiptText
import fintrack.proyecto4.onboarding.FirestoreOnboardingRepository
import fintrack.proyecto4.transaction.FirestoreTransactionRepository
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private val ComponentActivity.dataStore by preferencesDataStore(name = "fintrack_session")

class MainActivity : ComponentActivity() {

    // Debe registrarse durante la inicialización de la Activity (antes de STARTED),
    // por eso vive como propiedad y no dentro de un Composable.
    private var onImagePicked: ((String?) -> Unit)? = null
    private var onProfilePhotoPicked: ((String?) -> Unit)? = null

    /**
     * FinTrack es una app en español (Costa Rica) sin selector de idioma, pero componentes
     * de Material3 como el DatePicker (calendario del formulario de transacción, OCR, metas)
     * usan las cadenas localizadas que trae la librería según el locale del dispositivo. Si
     * el dispositivo/emulador está en inglés, el calendario sale en inglés aunque el resto de
     * la UI esté en español a mano. Forzar el locale de la app a es-CR aquí (antes de que se
     * infle cualquier recurso) soluciona esto en todos los DatePicker de la app de una vez,
     * en vez de tener que localizar cada uso por separado.
     */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("es", "CR")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val callback = onImagePicked
        onImagePicked = null
        callback?.invoke(uri?.let { copyUriToOcrFile(it) })
    }

    private val pickProfilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val callback = onProfilePhotoPicked
        onProfilePhotoPicked = null
        callback?.invoke(uri?.let { copyUriToOcrFile(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AndroidNotifierContext.appContext = applicationContext

        val sessionStore = DataStoreSessionStore(dataStore)
        val authRepository = FirebaseAuthRepository(sessionStore)
        val onboardingRepository = FirestoreOnboardingRepository()
        val budgetRepository = FirestoreBudgetRepository()
        val transactionRepository = FirestoreTransactionRepository()
        val notificationRepository = FirestoreNotificationRepository()

        setContent {
            App(
                authRepository = authRepository,
                onboardingRepository = onboardingRepository,
                budgetRepository = budgetRepository,
                transactionRepository = transactionRepository,
                notificationRepository = notificationRepository,
                ocrCameraContent = { onCaptured, onCancel ->
                    CameraXCaptureScreen(onCaptured = onCaptured, onCancel = onCancel)
                },
                onPickReceiptImage = { onPicked ->
                    onImagePicked = onPicked
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickProfilePhoto = { onPicked ->
                    onProfilePhotoPicked = onPicked
                    pickProfilePhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRecognizeReceiptText = { imagePath ->
                    recognizeReceiptText(applicationContext, imagePath)
                }
            )
        }
    }

    /**
     * El selector de fotos moderno (PickVisualMedia) entrega un content:// Uri de solo
     * lectura temporal; se copia al mismo directorio privado /files/ocr que usa la
     * captura de CameraX para que ML Kit lo procese de forma consistente.
     */
    private fun copyUriToOcrFile(uri: Uri): String? = try {
        val ocrDir = File(filesDir, "ocr").apply { if (!exists()) mkdirs() }
        val outputFile = File(ocrDir, "${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output -> input.copyTo(output) }
        }
        outputFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
