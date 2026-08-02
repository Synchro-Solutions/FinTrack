package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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

private const val GENERIC_CONFIRMATION_MESSAGE = "Si el email existe, recibirás un enlace en minutos"

@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit = {}
) {
    val viewModel = viewModel { ForgotPasswordViewModel(authRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    val montserrat = montserratFamily()

    val isLoading = uiState is ForgotPasswordUiState.Loading
    val sentState = uiState as? ForgotPasswordUiState.Sent
    val cooldownActive = sentState != null && sentState.secondsRemaining > 0
    val canSubmit = email.isNotBlank() && !isLoading && !cooldownActive

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

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WhiteAlpha10)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = White, fontSize = 20.sp)
                }
            }

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
                            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                        )

                        Text(
                            text = "Correo electrónico",
                            color = WhiteAlpha70,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = montserrat,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = BorderFocused,
                                unfocusedBorderColor = BorderDefault,
                                errorBorderColor = ErrorColor,
                                focusedContainerColor = WhiteAlpha10,
                                unfocusedContainerColor = WhiteAlpha10,
                                cursorColor = BorderFocused
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val errorMessage = (uiState as? ForgotPasswordUiState.Error)?.message
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = ErrorColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                            )
                        }

                        if (sentState != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GreenPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = GENERIC_CONFIRMATION_MESSAGE,
                                    color = GreenLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
                                    cooldownActive -> Text(
                                        text = "Reenviar en ${sentState.secondsRemaining}s",
                                        color = WhiteAlpha70,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = montserrat
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
            }
        }
    }
}
