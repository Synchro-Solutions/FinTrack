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
import fintrack.proyecto4.auth.RegisterUiState
import fintrack.proyecto4.auth.RegisterViewModel
import fintrack.proyecto4.navigation.LocalNavController
import fintrack.proyecto4.navigation.Screen
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
fun RegisterScreen(authRepository: AuthRepository) {
    val navController = LocalNavController.current
    val viewModel = viewModel { RegisterViewModel(authRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val montserrat = montserratFamily()

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            navController.replace(Screen.InitialConfig)
            viewModel.resetState()
        }
    }

    val isLoading = uiState is RegisterUiState.Loading
    val canSubmit = email.isNotBlank() && password.isNotBlank() && 
                    confirmPassword.isNotBlank() && password == confirmPassword && !isLoading

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Crea tu cuenta",
                color = White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = montserrat
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    FieldLabel("Correo electrónico")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.resetState() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("correo@ejemplo.com", color = WhiteAlpha40) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = greenTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FieldLabel("Contraseña")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.resetState() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = WhiteAlpha40) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Ocultar" else "Ver", color = GreenLight, fontSize = 12.sp)
                            }
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = greenTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    PasswordStrengthIndicator(password)

                    Spacer(modifier = Modifier.height(16.dp))

                    FieldLabel("Confirmar contraseña")
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.resetState() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = WhiteAlpha40) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            focusManager.clearFocus()
                            if (canSubmit) viewModel.register(email, password, confirmPassword)
                        }),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = greenTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text("Las contraseñas no coinciden", color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    val errorMessage = (uiState as? RegisterUiState.Error)?.message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.register(email, password, confirmPassword)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                brush = if (canSubmit) Brush.horizontalGradient(listOf(GreenDark, GreenPrimary, GreenLight))
                                        else Brush.horizontalGradient(listOf(GreenDark.copy(0.4f), GreenPrimary.copy(0.4f))),
                                shape = RoundedCornerShape(14.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) CircularProgressIndicator(color = White, strokeWidth = 2.dp)
                            else Text("Registrarse", color = White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TextButton(onClick = { navController.goBack() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = GreenLight, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(password: String) {
    if (password.isEmpty()) return
    
    val lengthOk = password.length >= 8
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    
    val strength = listOf(lengthOk, hasUpper, hasDigit).count { it }
    val (text, color) = when {
        strength == 3 -> "Fuerte" to GreenPrimary
        strength == 2 -> "Media" to Color.Yellow
        else -> "Débil" to ErrorColor
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(modifier = Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fortaleza: $text", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, color = WhiteAlpha70, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun greenTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White, unfocusedTextColor = White,
    focusedBorderColor = BorderFocused, unfocusedBorderColor = BorderDefault,
    focusedContainerColor = WhiteAlpha10, unfocusedContainerColor = WhiteAlpha10,
    cursorColor = BorderFocused
)
