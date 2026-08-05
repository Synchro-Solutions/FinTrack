package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import fintrack.proyecto4.onboarding.CURRENCIES
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.profile.EditarPerfilViewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.ShimmerText

@Composable
fun EditarPerfilScreen(
    uid: String,
    onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    onPickPhoto: (onPicked: (String?) -> Unit) -> Unit = { onPicked -> onPicked(null) },
    uploadPhoto: suspend (String) -> Result<String> = { Result.failure(UnsupportedOperationException()) },
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val c = LocalAppColors.current
    val viewModel = viewModel(key = uid) {
        EditarPerfilViewModel(onboardingRepository, uid, uploadPhoto)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var currencyExpanded by remember { mutableStateOf(false) }
    val selectedCurrency = CURRENCIES.firstOrNull { it.code == state.currency } ?: CURRENCIES.first()

    LaunchedEffect(state.savedOk) {
        if (state.savedOk) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── TopBar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceSecondary)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Volver",
                    tint = c.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            ShimmerText(
                text = "Editar perfil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                baseColor = c.textPrimary,
                accentColor = c.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Avatar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(c.surfaceSecondary)
                    .border(2.dp, c.border, CircleShape)
                    .clickable(enabled = !state.isUploadingPhoto) {
                        onPickPhoto { path -> if (path != null) viewModel.onPhotoPicked(path) }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isUploadingPhoto -> CircularProgressIndicator(
                        color = FinTrackColors.GreenPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )

                    state.photoUrl != null -> AsyncImage(
                        model = state.photoUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )

                    else -> Text(
                        text = state.name.take(2).uppercase().ifEmpty { "?" },
                        color = FinTrackColors.GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 68.dp, top = 68.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(FinTrackColors.GreenPrimary)
                    .clickable(enabled = !state.isUploadingPhoto) {
                        onPickPhoto { path -> if (path != null) viewModel.onPhotoPicked(path) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nombre visible") },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = editProfileFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.income,
                onValueChange = viewModel::setIncome,
                label = { Text("Ingreso mensual estimado") },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("${selectedCurrency.symbol} ", color = c.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = editProfileFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            Box {
                OutlinedTextField(
                    value = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.label}",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !state.isSaving,
                    label = { Text("Moneda principal") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = c.textSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isSaving) { currencyExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = editProfileFieldColors()
                )

                DropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false },
                    modifier = Modifier.background(c.surface)
                ) {
                    CURRENCIES.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${option.symbol} ${option.code} — ${option.label}",
                                    color = c.textPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                viewModel.setCurrency(option.code)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = FinTrackColors.ErrorColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(28.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving && !state.isUploadingPhoto && !state.isLoading,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackColors.GreenDark),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text("Guardar cambios", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun editProfileFieldColors() = run {
    val c = LocalAppColors.current
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FinTrackColors.GreenPrimary,
        unfocusedBorderColor = c.border,
        focusedLabelColor = FinTrackColors.GreenPrimary,
        unfocusedLabelColor = c.textSecondary,
        cursorColor = FinTrackColors.GreenPrimary,
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary
    )
}
