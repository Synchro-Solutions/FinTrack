package fintrack.proyecto4.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.ocr.OcrAssistantStatus
import fintrack.proyecto4.ocr.OcrAssistantViewModel
import fintrack.proyecto4.ocr.OcrResult
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily


@Composable
fun OcrAssistantScreen(
    viewModel: OcrAssistantViewModel,
    onBack: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onReviewData: (OcrResult) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        ScreenHeader(title = "Asistente OCR", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FinTrackColors.GreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = FinTrackColors.GreenPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Escanear comprobante",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Toma o sube una foto de tu factura, recibo o comprobante de pago",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = montserratFamily(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            when (state.status) {
                OcrAssistantStatus.Idle -> IdleContent(
                    onTakePhotoClick = onTakePhotoClick,
                    onPickImageClick = onPickImageClick
                )

                OcrAssistantStatus.Processing -> ProcessingContent()

                OcrAssistantStatus.Success -> SuccessContent(
                    onReviewData = { state.result?.let(onReviewData) }
                )

                OcrAssistantStatus.Error -> ErrorContent(
                    message = state.errorMessage,
                    onRetry = viewModel::reset
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    onTakePhotoClick: () -> Unit,
    onPickImageClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceSecondary)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .clickable(onClick = onTakePhotoClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Toca para tomar foto",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = montserratFamily()
                )
                Text(
                    text = "o selecciona una imagen",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = montserratFamily()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onTakePhotoClick,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tomar foto",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily()
                )
            }

            OutlinedButton(
                onClick = onPickImageClick,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Seleccionar",
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily()
                )
            }
        }
    }
}

@Composable
private fun ProcessingContent() {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 48.dp)
    ) {
        CircularProgressIndicator(
            color = FinTrackColors.GreenPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Procesando imagen...",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Detectando montos, fechas y comercio",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontFamily = montserratFamily()
        )
    }
}

@Composable
private fun SuccessContent(onReviewData: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FinTrackColors.GreenPrimary.copy(alpha = 0.12f))
                .border(1.dp, FinTrackColors.GreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = FinTrackColors.GreenPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Comprobante procesado exitosamente",
                color = FinTrackColors.GreenPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                fontFamily = montserratFamily(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onReviewData,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
        ) {
            Text(
                text = "Revisar datos detectados",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FinTrackColors.ErrorColor.copy(alpha = 0.12f))
                .border(1.dp, FinTrackColors.ErrorColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = FinTrackColors.ErrorColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message ?: "No se pudo procesar la imagen. Intenta de nuevo.",
                color = FinTrackColors.ErrorColor,
                fontSize = 13.sp,
                fontFamily = montserratFamily()
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenPrimary)
        ) {
            Text(
                text = "Reintentar",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = montserratFamily()
            )
        }
    }
}
