package fintrack.proyecto4

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import fintrack.proyecto4.auth.DataStoreSessionStore
import fintrack.proyecto4.auth.FirebaseAuthRepository
import fintrack.proyecto4.budget.FirestoreBudgetRepository
import fintrack.proyecto4.firebase.FirebaseEmulatorConfig
import fintrack.proyecto4.ocr.CameraXCaptureScreen
import fintrack.proyecto4.ocr.recognizeReceiptText
import fintrack.proyecto4.onboarding.FirestoreOnboardingRepository
import fintrack.proyecto4.profile.CloudinaryUploader
import fintrack.proyecto4.notifications.FirestoreNotificationRepository
import fintrack.proyecto4.transaction.FirestoreCustomCategoryRepository
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

        FirebaseEmulatorConfig.connectIfEnabled()

        val sessionStore = DataStoreSessionStore(dataStore)
        val authRepository = FirebaseAuthRepository(sessionStore)
        val onboardingRepository = FirestoreOnboardingRepository()
        val budgetRepository = FirestoreBudgetRepository()
        val transactionRepository = FirestoreTransactionRepository()
        val categoryRepository = FirestoreCustomCategoryRepository()
        val notificationRepository = FirestoreNotificationRepository()

        setContent {
            App(
                authRepository = authRepository,
                onboardingRepository = onboardingRepository,
                budgetRepository = budgetRepository,
                transactionRepository = transactionRepository,
                categoryRepository = categoryRepository,
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
                },
                onShareText = { text -> shareText(text) },
                onUploadProfilePhoto = { path -> CloudinaryUploader.uploadProfilePhoto(path) },
                onUploadReceiptPhoto = { path -> CloudinaryUploader.uploadReceiptPhoto(path) },
                onGoogleSignInRequested = { requestGoogleIdToken() }
            )
        }
    }

    private fun shareText(text: String) {
        if (text.isBlank()) return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, "Compartir resumen"))
    }

    /**
     * Pide un ID token de Google vía Credential Manager (reemplaza al deprecado
     * GoogleSignInClient) y lo entrega para intercambiarlo por credenciales de Firebase.
     * Requiere que R.string.google_web_client_id tenga el Web client ID real de Firebase
     * (Authentication > Sign-in method > Google), no el placeholder por defecto.
     *
     * GetGoogleIdOption (One Tap) solo puede ofrecer cuentas ya guardadas por Credential
     * Manager en el dispositivo; si no hay ninguna (dispositivo nuevo, o ninguna cuenta
     * Google agregada) falla con "Cannot find a matching credential" en vez de mostrar un
     * selector. Por eso, si falla, se reintenta con GetSignInWithGoogleOption, que sí abre
     * el flujo completo de elegir/agregar cuenta.
     */
    private suspend fun requestGoogleIdToken(): Result<String> {
        val webClientId = getString(R.string.google_web_client_id)
        val credentialManager = CredentialManager.create(this)

        val oneTapOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        return try {
            requestCredential(credentialManager, oneTapOption)
        } catch (e: GetCredentialException) {
            val fallbackOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            try {
                requestCredential(credentialManager, fallbackOption)
            } catch (fallbackError: GetCredentialException) {
                Result.failure(fallbackError)
            } catch (fallbackError: GoogleIdTokenParsingException) {
                Result.failure(fallbackError)
            }
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(e)
        }
    }

    private suspend fun requestCredential(
        credentialManager: CredentialManager,
        option: CredentialOption
    ): Result<String> {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = credentialManager.getCredential(this, request)
        val credential = response.credential
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Result.success(googleIdTokenCredential.idToken)
        } else {
            Result.failure(IllegalStateException("Credencial inesperada de Google"))
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
