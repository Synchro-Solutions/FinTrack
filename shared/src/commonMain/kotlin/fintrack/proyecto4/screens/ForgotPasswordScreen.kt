package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.auth.ForgotPasswordUiState
import fintrack.proyecto4.auth.ForgotPasswordViewModel
import fintrack.proyecto4.theme.FinTrackColors.BorderDefault
import fintrack.proyecto4.theme.FinTrackColors.BorderFocused
import fintrack.proyecto4.theme.FinTrackColors.CardBackground
import fintrack.proyecto4.theme.FinTrackColors.ErrorColor
import fintrack.proyecto4.theme.FinTrackColors.GreenDark
import fintrack.proyecto4.theme.FinTrackColors.GreenLight
import fintrack.proyecto4.theme.FinTrackColors.GreenPrimary
import fintrack.proyecto4.theme.FinTrackColors.OverlayDark
import fintrack.proyecto4.theme.FinTrackColors.OverlayDarkBottom
import fintrack.proyecto4.theme.FinTrackColors.OverlayGreenMid
import fintrack.proyecto4.theme.FinTrackColors.White
import fintrack.proyecto4.theme.FinTrackColors.WhiteAlpha10
import fintrack.proyecto4.theme.FinTrackColors.WhiteAlpha40
import fintrack.proyecto4.theme.FinTrackColors.WhiteAlpha70
import fintrack.proyecto4.theme.montserratFamily
import fintrack.shared.generated.resources.Res
import fintrack.shared.generated.resources.login_background
import org.jetbrains.compose.resources.painterResource

private const val CONFIRMATION_MESSAGE = "Si el email existe, recibirás un enlace en minutos"

@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit = {}
) {
    val viewModel = viewModel { ForgotPasswordViewModel(authRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsStateWithLifecycle()
    val montserrat = montserratFamily()

    var email by remember { mutableStateOf("") }

    val isLoading = uiState is ForgotPasswordUiState.Loading
    val isSent = uiState is ForgotPasswordUiState.Sent
    val isOnCooldown = cooldownSeconds > 0
    val canSubmit = email.isNotBlank() && !isLoading && !isOnCooldown

    Box(modifier = Modifier.fillMaxSize()) {

        androidx.compose.foundation.Image(
            painter = painterResource(Res.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to OverlayDark,
                            0.4f to OverlayGreenMid,
                            1.0f to OverlayDarkBottom
                        )
                    )
                )
        )

        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            tint = White,
            modifier = Modifier
                .padding(top = 20.dp, start = 20.dp)
                .size(28.dp)
                .clickable(onClick = onBack)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                    Text(
                        text = "Ingresa tu correo y te enviaremos un enlace para restablecerla",
                        color = WhiteAlpha70,
                        fontSize = 13.sp,
                        fontFamily = montserrat,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                    )

                    if (isSent) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GreenPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = GreenLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = CONFIRMATION_MESSAGE,
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = montserrat,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Revisa también tu carpeta de spam. El enlace expira en 1 hora.",
                                color = WhiteAlpha70,
                                fontSize = 12.sp,
                                fontFamily = montserrat,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    FieldLabelForgot("Correo electrónico")
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("correo@ejemplo.com", color = WhiteAlpha40) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canSubmit) viewModel.sendResetLink(email) }
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = uiState is ForgotPasswordUiState.Error,
                        colors = greenTextFieldColorsForgot(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Validación inline / error
                    val errorMessage = (uiState as? ForgotPasswordUiState.Error)?.message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = ErrorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.sendResetLink(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (canSubmit) {
                                        Brush.horizontalGradient(
                                            colors = listOf(GreenDark, GreenPrimary, GreenLight)
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                GreenDark.copy(alpha = 0.4f),
                                                GreenPrimary.copy(alpha = 0.4f)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isLoading -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.5.dp
                                )
                                isOnCooldown -> Text(
                                    text = "Reenviar en ${cooldownSeconds}s",
                                    color = White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = montserrat
                                )
                                isSent -> Text(
                                    text = "Enviar de nuevo",
                                    color = White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = montserrat,
                                    letterSpacing = 0.5.sp
                                )
                                else -> Text(
                                    text = "Enviar enlace",
                                    color = White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = montserrat,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya la recordaste? ",
                    color = WhiteAlpha70,
                    fontSize = 13.sp,
                    fontFamily = montserrat
                )
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Volver a iniciar sesión",
                        color = GreenLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabelForgot(text: String) {
    Text(
        text = text,
        color = WhiteAlpha70,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = montserratFamily(),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
    )
}

@Composable
private fun greenTextFieldColorsForgot() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedBorderColor = BorderFocused,
    unfocusedBorderColor = BorderDefault,
    errorBorderColor = ErrorColor,
    focusedContainerColor = WhiteAlpha10,
    unfocusedContainerColor = WhiteAlpha10,
    cursorColor = BorderFocused
)