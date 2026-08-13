package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegistered: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val viewModel = viewModel { RegisterViewModel(authRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val montserrat = montserratFamily()

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            viewModel.resetState()
            onRegistered()
        }
    }

    val isLoading = uiState is RegisterUiState.Loading
    val canSubmit = email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank() &&
        acceptedTerms && !isLoading

    fun submit() {
        focusManager.clearFocus()
        viewModel.register(email, password, confirm)
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text(
                        text = "Crear cuenta",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                    Text(
                        text = "Regístrate para empezar a ordenar tu dinero",
                        color = WhiteAlpha70,
                        fontSize = 13.sp,
                        fontFamily = montserrat,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                    )

                    FieldLabelRegister("Correo electrónico")
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState is RegisterUiState.Error) viewModel.resetState()
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
                        colors = registerTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FieldLabelRegister("Contraseña")
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is RegisterUiState.Error) viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mínimo 6 caracteres", color = WhiteAlpha40) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
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
                        colors = registerTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FieldLabelRegister("Confirmar contraseña")
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            confirm = it
                            if (uiState is RegisterUiState.Error) viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Repite la contraseña", color = WhiteAlpha40) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canSubmit) submit() }
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = registerTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Aceptación de Términos y Condiciones (requerido para registrarse).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = GreenPrimary,
                                uncheckedColor = WhiteAlpha40,
                                checkmarkColor = White
                            )
                        )
                        Text(
                            text = "Acepto los ",
                            color = WhiteAlpha70,
                            fontSize = 13.sp,
                            fontFamily = montserrat
                        )
                        Text(
                            text = "Términos y Condiciones",
                            color = GreenLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = montserrat,
                            modifier = Modifier.clickable { showTerms = true }
                        )
                    }

                    val errorMessage = (uiState as? RegisterUiState.Error)?.message
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
                            Text("⚠", color = ErrorColor, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(errorMessage, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { submit() },
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
                                        Brush.horizontalGradient(listOf(GreenDark, GreenPrimary, GreenLight))
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(GreenDark.copy(alpha = 0.4f), GreenPrimary.copy(alpha = 0.4f))
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
                                    text = "Crear cuenta",
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
                    text = "¿Ya tienes cuenta? ",
                    color = WhiteAlpha70,
                    fontSize = 13.sp,
                    fontFamily = montserrat
                )
                TextButton(
                    onClick = onBack,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Inicia sesión",
                        color = GreenLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserrat
                    )
                }
            }
        }

        if (showTerms) {
            TermsDialog(
                onAccept = {
                    acceptedTerms = true
                    showTerms = false
                },
                onDismiss = { showTerms = false }
            )
        }
    }
}

@Composable
private fun TermsDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Condiciones") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = TERMS_AND_CONDITIONS,
                    fontSize = 13.sp,
                    fontFamily = montserratFamily()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Aceptar", color = GreenPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

private const val TERMS_AND_CONDITIONS =
    "Bienvenido a FinTrack. Al crear una cuenta aceptas los siguientes términos:\n\n" +
        "1. Uso del servicio. FinTrack es una herramienta de apoyo para la gestión de tus " +
        "finanzas personales. La información y los cálculos (aguinaldo, salario neto, etc.) son " +
        "estimaciones de carácter informativo y no constituyen asesoría financiera, legal ni " +
        "contable profesional.\n\n" +
        "2. Tu cuenta. Eres responsable de la veracidad de los datos que ingresas y de mantener " +
        "la confidencialidad de tus credenciales. La actividad realizada desde tu cuenta es tu " +
        "responsabilidad.\n\n" +
        "3. Privacidad de datos. Tus datos financieros se almacenan de forma segura y se usan " +
        "únicamente para brindarte el servicio dentro de la app. No compartimos tu información " +
        "personal con terceros con fines comerciales.\n\n" +
        "4. Disponibilidad. El servicio se ofrece \"tal cual\", pudiendo presentar interrupciones " +
        "o cambios. FinTrack no se hace responsable por decisiones tomadas con base en la " +
        "información mostrada.\n\n" +
        "5. Aceptación. Al marcar la casilla y crear tu cuenta confirmas que leíste y aceptas " +
        "estos Términos y Condiciones y la Política de Privacidad.\n\n" +
        "Última actualización: 2026."

@Composable
private fun FieldLabelRegister(text: String) {
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
private fun registerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedBorderColor = BorderFocused,
    unfocusedBorderColor = BorderDefault,
    focusedContainerColor = WhiteAlpha10,
    unfocusedContainerColor = WhiteAlpha10,
    cursorColor = BorderFocused
)
