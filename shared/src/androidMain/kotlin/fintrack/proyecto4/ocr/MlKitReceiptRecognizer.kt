package fintrack.proyecto4.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


suspend fun recognizeReceiptText(context: Context, imagePath: String): String =
    suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }
