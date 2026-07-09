package fintrack.proyecto4.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily

/**
 * Snackbar de confirmación (ej. "Movimiento registrado exitosamente") con la misma
 * identidad visual del resto de la app: acento verde, tipografía Montserrat, y colores
 * que se adaptan al tema activo (claro/oscuro) vía [LocalAppColors] — mismo criterio que
 * usa el banner de advertencia de OcrConfirmScreen.
 */
@Composable
fun SuccessSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        SuccessSnackbar(message = data.visuals.message)
    }
}

@Composable
private fun SuccessSnackbar(message: String) {
    val colors = LocalAppColors.current
    val background = if (colors.isDark) FinTrackColors.GreenPrimary.copy(alpha = 0.16f) else colors.surface
    val border = if (colors.isDark) FinTrackColors.GreenPrimary.copy(alpha = 0.4f) else FinTrackColors.GreenPrimary.copy(alpha = 0.3f)
    val textColor = if (colors.isDark) FinTrackColors.GreenLight else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FinTrackColors.GreenPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserratFamily()
        )
    }
}
