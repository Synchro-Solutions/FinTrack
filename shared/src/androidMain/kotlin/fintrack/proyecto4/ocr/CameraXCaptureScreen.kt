package fintrack.proyecto4.ocr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import fintrack.proyecto4.theme.FinTrackColors
import java.io.File
import java.io.FileOutputStream

@Composable
fun CameraXCaptureScreen(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            hasPermission -> CameraPreviewWithShutter(onCaptured = onCaptured, onCancel = onCancel)
            permissionDenied -> PermissionRequiredDialog(
                onDismiss = onCancel,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onCancel()
                }
            )
        }
    }
}

@Composable
private fun CameraPreviewWithShutter(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1080, 1920), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER)
                    )
                    .build()
            )
            .build()
    }

    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            captureError = "No se pudo iniciar la cámara"
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancelar",
                tint = Color.White,
                modifier = Modifier
                    .padding(20.dp)
                    .size(28.dp)
                    .clickable(onClick = onCancel)
            )

            captureError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCapturing) FinTrackColors.GreenPrimary.copy(alpha = 0.5f)
                        else FinTrackColors.GreenPrimary
                    )
                    .let { base ->
                        if (isCapturing) base else base.clickable {
                            isCapturing = true
                            captureError = null
                            capturePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                onSuccess = { path ->
                                    isCapturing = false
                                    onCaptured(path)
                                },
                                onError = {
                                    isCapturing = false
                                    captureError = "No se pudo capturar la imagen"
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Capturar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permiso de cámara requerido") },
        text = {
            Text("FinTrack necesita acceso a la cámara para escanear comprobantes. Actívalo en los ajustes de la aplicación.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Ir a Ajustes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val ocrDir = File(context.filesDir, "ocr").apply { if (!exists()) mkdirs() }
    val outputFile = File(ocrDir, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {

                    compressToMaxSize(outputFile, maxBytes = 2 * 1024 * 1024, initialQuality = 85)
                    onSuccess(outputFile.absolutePath)
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun compressToMaxSize(file: File, maxBytes: Long, initialQuality: Int) {
    if (file.length() <= maxBytes) return

    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
    var quality = initialQuality
    var bytes: ByteArray

    do {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        bytes = stream.toByteArray()
        quality -= 10
    } while (bytes.size > maxBytes && quality > 30)

    FileOutputStream(file).use { it.write(bytes) }
    bitmap.recycle()
}
