package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.budget.BUDGET_CATEGORIES
import fintrack.proyecto4.budget.BudgetCategory
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.CreateBudgetState
import fintrack.proyecto4.budget.CreateBudgetViewModel
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.budget.colorFromHex
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.util.formatColones
import kotlin.math.roundToInt

@Composable
fun CreateBudgetScreen(
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = "create_$uid") {
        CreateBudgetViewModel(budgetRepository, uid)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinTrackColors.BgApp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = FinTrackColors.TextPrimary
                )
            }
            Text(
                text = "Nuevo presupuesto",
                color = FinTrackColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Categoría
            SectionLabel("CATEGORÍA *")
            Spacer(Modifier.height(12.dp))
            CategoryGrid(
                selectedCategory = state.selectedCategory,
                onSelect = viewModel::selectCategory
            )

            Spacer(Modifier.height(24.dp))

            // Monto límite
            SectionLabel("MONTO LÍMITE (₡) *")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.limitAmount,
                onValueChange = viewModel::setLimitAmount,
                placeholder = { Text("150000", color = FinTrackColors.TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinTrackColors.GreenPrimary,
                    unfocusedBorderColor = FinTrackColors.BorderDefault,
                    focusedTextColor = FinTrackColors.TextPrimary,
                    unfocusedTextColor = FinTrackColors.TextPrimary,
                    cursorColor = FinTrackColors.GreenPrimary,
                    focusedContainerColor = FinTrackColors.SurfacePrimary,
                    unfocusedContainerColor = FinTrackColors.SurfacePrimary
                )
            )
            if (state.limitAmount.isNotBlank()) {
                val parsed = state.limitAmount.toDoubleOrNull() ?: 0.0
                Text(
                    text = formatColones(parsed.toLong()),
                    color = FinTrackColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Alerta
            AlertSlider(
                threshold = state.alertThreshold,
                onThresholdChange = viewModel::setAlertThreshold
            )

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = FinTrackColors.ErrorColor,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        // Botón crear
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = state.canSave && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    disabledContainerColor = FinTrackColors.GreenPrimary.copy(alpha = 0.4f)
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
                        text = "Crear presupuesto",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ── Sección: etiqueta ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = FinTrackColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

// ── Grid de categorías ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    selectedCategory: BudgetCategory?,
    onSelect: (BudgetCategory) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BUDGET_CATEGORIES.forEach { cat ->
            val isSelected = cat.name == selectedCategory?.name
            val catColor = colorFromHex(cat.colorHex)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) catColor.copy(alpha = 0.18f)
                        else FinTrackColors.SurfacePrimary
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) catColor else FinTrackColors.BorderDefault,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = cat.icon, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cat.name,
                        color = if (isSelected) catColor else FinTrackColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Slider de alerta ───────────────────────────────────────────────────────

@Composable
private fun AlertSlider(
    threshold: Float,
    onThresholdChange: (Float) -> Unit
) {
    val pct = (threshold * 100).roundToInt()

    SectionLabel("ALERTA AL $pct% USADO")
    Spacer(Modifier.height(8.dp))

    Slider(
        value = threshold,
        onValueChange = onThresholdChange,
        valueRange = 0.5f..0.95f,
        steps = 8,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = FinTrackColors.GreenPrimary,
            activeTrackColor = FinTrackColors.GreenPrimary,
            inactiveTrackColor = FinTrackColors.SurfaceSecondary
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("50%", color = FinTrackColors.TextSecondary, fontSize = 11.sp)
        Text("95%", color = FinTrackColors.TextSecondary, fontSize = 11.sp)
    }
}
