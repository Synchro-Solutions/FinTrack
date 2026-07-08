package fintrack.proyecto4.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import fintrack.proyecto4.onboarding.CurrencyOption
import fintrack.proyecto4.onboarding.OnboardingState
import fintrack.proyecto4.onboarding.OnboardingViewModel
import fintrack.proyecto4.theme.FinTrackColors

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onPickPhoto: ((String?) -> Unit) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp)
        ) {
            StepIndicator(currentStep = state.step)

            Spacer(Modifier.height(40.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(tween(280)) { if (forward) it else -it } +
                            fadeIn(tween(280))
                    val exit = slideOutHorizontally(tween(280)) { if (forward) -it else it } +
                            fadeOut(tween(280))
                    enter.togetherWith(exit)
                },
                modifier = Modifier.weight(1f),
                label = "OnboardingStep"
            ) { step ->
                when (step) {
                    1 -> Step1Content(
                        state = state,
                        onNameChange = viewModel::setName,
                        onPickPhoto = { onPickPhoto { path -> viewModel.setPhoto(path) } }
                    )
                    2 -> Step2Content(
                        state = state,
                        onIncomeChange = viewModel::setIncome,
                        onCurrencyChange = viewModel::setCurrency
                    )
                    else -> Step3Content(
                        state = state,
                        onPrivacyChange = viewModel::setPrivacy,
                        onTermsChange = viewModel::setTerms
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error!!,
                    color = FinTrackColors.ErrorColor,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.step > 1) {
                    OutlinedButton(
                        onClick = viewModel::goBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FinTrackColors.TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, FinTrackColors.BorderDefault
                        )
                    ) {
                        Text("Anterior", fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = {
                        if (state.step == 3) viewModel.saveAndFinish(onFinished)
                        else viewModel.goNext()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinTrackColors.GreenPrimary,
                        disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.4f)
                    ),
                    enabled = when (state.step) {
                        1 -> viewModel.canProceedStep1()
                        3 -> viewModel.canFinish() && !state.isSaving
                        else -> true
                    }
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (state.step == 3) "Empezar" else "Siguiente",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            val stepNum = index + 1
            val isDone = stepNum < currentStep
            val isActive = stepNum == currentStep

            StepCircle(number = stepNum, isDone = isDone, isActive = isActive)

            if (index < 2) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(2.dp)
                        .background(
                            if (stepNum < currentStep) FinTrackColors.GreenPrimary
                            else FinTrackColors.DividerColor
                        )
                )
            }
        }
    }
}

@Composable
private fun StepCircle(number: Int, isDone: Boolean, isActive: Boolean) {
    val bg = when {
        isDone -> FinTrackColors.GreenPrimary
        isActive -> FinTrackColors.GreenPrimary
        else -> FinTrackColors.SurfaceSecondary
    }
    val border = when {
        isDone || isActive -> FinTrackColors.GreenPrimary
        else -> FinTrackColors.DividerColor
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = "$number",
                color = if (isActive) Color.White else FinTrackColors.TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ── Step 1: Nombre y foto ──────────────────────────────────────────────────

@Composable
private fun Step1Content(
    state: OnboardingState,
    onNameChange: (String) -> Unit,
    onPickPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cuéntanos de ti",
            color = FinTrackColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            text = "Así personalizamos tu experiencia",
            color = FinTrackColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(FinTrackColors.SurfaceSecondary)
                .border(2.dp, FinTrackColors.BorderDefault, CircleShape)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center
        ) {
            if (state.photoPath != null) {
                Text(
                    text = state.name.take(2).uppercase().ifEmpty { "?" },
                    color = FinTrackColors.GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = FinTrackColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Foto",
                        color = FinTrackColors.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Nombre visible") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = onboardingFieldColors()
        )
    }
}

// ── Step 2: Ingreso mensual y moneda ──────────────────────────────────────

@Composable
private fun Step2Content(
    state: OnboardingState,
    onIncomeChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    var currencyExpanded by remember { mutableStateOf(false) }
    val selectedCurrency = CURRENCIES.firstOrNull { it.code == state.currency } ?: CURRENCIES.first()
    val selectedCurrencyLabel = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.label}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Tu situación financiera",
            color = FinTrackColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            text = "Úsalo como referencia para tus metas",
            color = FinTrackColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.income,
            onValueChange = { value ->
                if (value.all { it.isDigit() || it == '.' }) onIncomeChange(value)
            },
            label = { Text("Ingreso mensual estimado") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = onboardingFieldColors(),
            prefix = {
                Text(
                    text = "${selectedCurrency.symbol} ",
                    color = FinTrackColors.TextSecondary
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Box {
            OutlinedTextField(
                value = selectedCurrencyLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Moneda principal") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = FinTrackColors.TextSecondary
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
                modifier = Modifier.background(FinTrackColors.SurfacePrimary)
            ) {
                CURRENCIES.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${option.symbol} ${option.code} — ${option.label}",
                                color = FinTrackColors.TextPrimary,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onCurrencyChange(option.code)
                            currencyExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = FinTrackColors.TextPrimary
                        )
                    )
                }
            }
        }
    }
}

// ── Step 3: Consentimientos ────────────────────────────────────────────────

@Composable
private fun Step3Content(
    state: OnboardingState,
    onPrivacyChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Antes de empezar",
            color = FinTrackColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Text(
            text = "Lee y acepta los siguientes documentos",
            color = FinTrackColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(32.dp))

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
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FinTrackColors.SurfacePrimary)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = FinTrackColors.GreenPrimary,
                uncheckedColor = FinTrackColors.TextSecondary,
                checkmarkColor = Color.White
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = FinTrackColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun onboardingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FinTrackColors.GreenPrimary,
    unfocusedBorderColor = FinTrackColors.BorderDefault,
    focusedLabelColor = FinTrackColors.GreenPrimary,
    unfocusedLabelColor = FinTrackColors.TextSecondary,
    cursorColor = FinTrackColors.GreenPrimary,
    focusedTextColor = FinTrackColors.TextPrimary,
    unfocusedTextColor = FinTrackColors.TextPrimary
)
