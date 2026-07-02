package fintrack.proyecto4

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
import fintrack.proyecto4.ocr.CameraXCaptureScreen
import fintrack.proyecto4.ocr.recognizeReceiptText
import java.io.File
import java.io.FileOutputStream

private val ComponentActivity.dataStore by preferencesDataStore(name = "fintrack_session")

class MainActivity : ComponentActivity() {

    // Debe registrarse durante la inicialización de la Activity (antes de STARTED),
    // por eso vive como propiedad y no dentro de un Composable.
    private var onImagePicked: ((String?) -> Unit)? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val callback = onImagePicked
        onImagePicked = null
        callback?.invoke(uri?.let { copyUriToOcrFile(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sessionStore = DataStoreSessionStore(dataStore)
        val authRepository = FirebaseAuthRepository(sessionStore)

        setContent {
            App(
                authRepository = authRepository,
                ocrCameraContent = { onCaptured, onCancel ->
                    CameraXCaptureScreen(onCaptured = onCaptured, onCancel = onCancel)
                },
                onPickReceiptImage = { onPicked ->
                    onImagePicked = onPicked
                    pickImageLauncher.launch(
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
