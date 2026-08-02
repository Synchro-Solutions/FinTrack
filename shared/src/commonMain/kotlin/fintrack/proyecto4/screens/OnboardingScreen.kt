package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fintrack.proyecto4.onboarding.CURRENCIES
import fintrack.proyecto4.onboarding.OnboardingState
import fintrack.proyecto4.onboarding.OnboardingViewModel
import fintrack.proyecto4.screens.common.SuccessSnackbarHost
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onPickPhoto: ((String?) -> Unit) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) {
            snackbarHostState.showSnackbar(
                message = "¡Todo listo! Tu configuración inicial se guardó correctamente.",
                duration = SnackbarDuration.Short
            )
            delay(900)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Configura tu cuenta",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Cuéntanos un poco de ti para personalizar FinTrack. Solo toma un minuto.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                FormCard {
                    AboutYouSection(
                        state = state,
                        onNameChange = viewModel::setName,
                        onPickPhoto = { onPickPhoto { path -> viewModel.setPhoto(path) } }
                    )
                }

                Spacer(Modifier.height(20.dp))

                FormCard {
                    FinancesSection(
                        state = state,
                        onIncomeChange = viewModel::setIncome,
                        onCurrencyChange = viewModel::setCurrency
                    )
                }

                Spacer(Modifier.height(20.dp))

                FormCard {
                    ConsentsSection(
                        state = state,
                        onPrivacyChange = viewModel::setPrivacy,
                        onTermsChange = viewModel::setTerms
                    )
                }

                if (state.error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.error!!,
                        color = FinTrackColors.ErrorColor,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackColors.GreenDark,
                        disabledContainerColor = FinTrackColors.GreenDark.copy(alpha = 0.4f)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Finalizar configuración",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        SuccessSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

// ── Tarjeta: Sobre ti (nombre + foto) ──────────────────────────────────────

@Composable
private fun AboutYouSection(
    state: OnboardingState,
    onNameChange: (String) -> Unit,
    onPickPhoto: () -> Unit
) {
    val colors = LocalAppColors.current
    FormSectionTitle("Sobre ti")

    Spacer(Modifier.height(16.dp))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary)
                .border(2.dp, colors.border, CircleShape)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center
        ) {
            if (state.photoPath != null) {
                Text(
                    text = state.name.take(2).uppercase().ifEmpty { "?" },
                    color = FinTrackColors.GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Foto",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    FieldLabel("Nombre visible *")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        placeholder = { Text("¿Cómo quieres que te llamemos?") },
        singleLine = true,
        isError = state.nameError != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = onboardingFieldColors()
    )
    if (state.nameError != null) {
        Text(
            text = state.nameError!!,
            color = FinTrackColors.ErrorColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}

// ── Tarjeta: Tu situación financiera (ingreso + moneda) ────────────────────

@Composable
private fun FinancesSection(
    state: OnboardingState,
    onIncomeChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var currencyExpanded by remember { mutableStateOf(false) }
    val selectedCurrency = CURRENCIES.firstOrNull { it.code == state.currency } ?: CURRENCIES.first()
    val selectedCurrencyLabel = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.label}"

    FormSectionTitle("Tu situación financiera")
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Úsalo como referencia para tus metas. Puedes ajustarlo después.",
        color = colors.textSecondary,
        fontSize = 12.sp
    )

    Spacer(Modifier.height(16.dp))

    FieldLabel("Ingreso mensual estimado")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.income,
        onValueChange = { value ->
            if (value.all { it.isDigit() || it == '.' }) onIncomeChange(value)
        },
        placeholder = { Text("0.00", color = colors.textSecondary) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = onboardingFieldColors(),
        prefix = {
            Text(
                text = "${selectedCurrency.symbol} ",
                color = colors.textSecondary
            )
        }
    )

    Spacer(Modifier.height(16.dp))

    FieldLabel("Moneda principal")
    Spacer(Modifier.height(8.dp))
    Box {
        OutlinedTextField(
            value = selectedCurrencyLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { currencyExpanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = onboardingFieldColors()
        )

        DropdownMenu(
            expanded = currencyExpanded,
            onDismissRequest = { currencyExpanded = false },
            modifier = Modifier.background(colors.surface)
        ) {
            CURRENCIES.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${option.symbol} ${option.code} — ${option.label}",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onCurrencyChange(option.code)
                        currencyExpanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = colors.textPrimary
                    )
                )
            }
        }
    }
}

// ── Tarjeta: Antes de empezar (consentimientos) ─────────────────────────────

@Composable
private fun ConsentsSection(
    state: OnboardingState,
    onPrivacyChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit
) {
    FormSectionTitle("Antes de empezar")
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Lee y acepta los siguientes documentos para continuar.",
        color = LocalAppColors.current.textSecondary,
        fontSize = 12.sp
    )

    Spacer(Modifier.height(16.dp))

    ConsentRow(
        checked = state.privacyAccepted,
        onCheckedChange = onPrivacyChange,
        label = "He leído y acepto la Política de Privacidad"
    )

    Spacer(Modifier.height(12.dp))

    ConsentRow(
        checked = state.termsAccepted,
        onCheckedChange = onTermsChange,
        label = "He leído y acepto los Términos y Condiciones"
    )

    if (state.consentsError != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.consentsError!!,
            color = FinTrackColors.ErrorColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSecondary)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = FinTrackColors.GreenPrimary,
                uncheckedColor = colors.textSecondary,
                checkmarkColor = Color.White
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

// ── Helpers visuales compartidos por las tres tarjetas ──────────────────────

@Composable
private fun FormSectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.textPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FieldLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun FormCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun onboardingFieldColors() = run {
    val colors = LocalAppColors.current
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FinTrackColors.GreenPrimary,
        unfocusedBorderColor = colors.border,
        errorBorderColor = FinTrackColors.ErrorColor,
        focusedLabelColor = FinTrackColors.GreenPrimary,
        unfocusedLabelColor = colors.textSecondary,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary
    )
}
