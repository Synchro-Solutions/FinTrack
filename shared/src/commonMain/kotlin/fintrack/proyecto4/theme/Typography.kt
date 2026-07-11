package fintrack.proyecto4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fintrack.shared.generated.resources.*
import org.jetbrains.compose.resources.Font

@Composable
fun montserratFamily() = FontFamily(
    Font(Res.font.montserrat_regular, weight = FontWeight.Normal),
    Font(Res.font.montserrat_medium, weight = FontWeight.Medium),
    Font(Res.font.montserrat_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, weight = FontWeight.Bold),
    Font(Res.font.montserrat_extrabold, weight = FontWeight.ExtraBold),
    Font(Res.font.montserrat_black, weight = FontWeight.Black),
)

@Composable
fun FinTrackTypography(): Typography {
    val montserrat = montserratFamily()
    return Typography(
        displayLarge  = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Black,  fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp),
        displaySmall  = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Bold,   fontSize = 36.sp),
        headlineLarge = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Bold,   fontSize = 32.sp),
        headlineMedium= TextStyle(fontFamily = montserrat, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        titleLarge    = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Bold,   fontSize = 22.sp),
        titleMedium   = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall    = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge     = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium    = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall     = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge    = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium   = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall    = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    )
}
