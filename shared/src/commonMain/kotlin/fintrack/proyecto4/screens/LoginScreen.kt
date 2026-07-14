package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthRepository
import fintrack.proyecto4.auth.LoginUiState
import fintrack.proyecto4.auth.LoginViewModel
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.Screen
import fintrack.proyecto4.theme.FinTrackColors
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

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit = {}
) {
    val navController = LocalNavController.current
    val viewModel = viewModel { LoginViewModel(authRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val montserrat = montserratFamily()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    val isLoading = uiState is LoginUiState.Loading
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !isLoading

    Box(modifier = Modifier.fillMaxSize()) {

        // Imagen de fondo
        androidx.compose.foundation.Image(
            painter = painterResource(Res.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Máscara oscura con degradado verde al negro
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

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo / Título
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GreenLight, GreenDark)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FinTrack",
                    color = White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = montserrat,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Gestión financiera personal",
                    color = WhiteAlpha70,
                    fontSize = 14.sp,
                    fontFamily = montserrat,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Card del formulario
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Text(
                        text = "Bienvenido de nuevo",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                    Text(
                        text = "Inicia sesión para continuar",
                        color = WhiteAlpha70,
                        fontSize = 13.sp,
                        fontFamily = montserrat,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                    )

                    // Email
                    FieldLabel("Correo electrónico")
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState is LoginUiState.Error || uiState is LoginUiState.Locked) {
                                viewModel.resetState()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("correo@ejemplo.com", color = WhiteAlpha40) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = greenTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contraseña
                    FieldLabel("Contraseña")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is LoginUiState.Error || uiState is LoginUiState.Locked) {
                                viewModel.resetState()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = WhiteAlpha40) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (canSubmit) viewModel.signIn(email, password, rememberMe)
                            }
                        ),
                        trailingIcon = {
                            TextButton(
                                onClick = { showPassword = !showPassword },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (showPassword) "Ocultar" else "Ver",
                                    color = GreenLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = greenTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recordarme
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = GreenPrimary,
                                uncheckedColor = WhiteAlpha40,
                                checkmarkColor = White
                            )
                        )
                        Text(
                            text = "Recordarme",
                            color = WhiteAlpha70,
                            fontSize = 14.sp
                        )
                    }

                    // Error / Bloqueo
                    val errorMessage = when (val state = uiState) {
                        is LoginUiState.Error -> state.message
                        is LoginUiState.Locked -> "Cuenta bloqueada, intente en ${state.minutesRemaining} minutos"
                        else -> null
                    }
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorColor.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠",
                                color = ErrorColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = ErrorColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón principal
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.signIn(email, password, rememberMe)
                        },
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
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Iniciar sesión",
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

            // Crear cuenta
            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta? ",
                    color = WhiteAlpha70,
                    fontSize = 13.sp,
                    fontFamily = montserratFamily()
                )
                TextButton(
                    onClick = { /* TODO: navegar a registro */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Crear cuenta",
                        color = GreenLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserratFamily()
                    )
                }
            }

            // Pie
            Text(
                text = "Tu dinero, bajo control.",
                color = WhiteAlpha40,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
                letterSpacing = 0.5.sp,
                fontFamily = montserratFamily()
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
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
private fun greenTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedBorderColor = BorderFocused,
    unfocusedBorderColor = BorderDefault,
    focusedContainerColor = WhiteAlpha10,
    unfocusedContainerColor = WhiteAlpha10,
    cursorColor = BorderFocused
)
