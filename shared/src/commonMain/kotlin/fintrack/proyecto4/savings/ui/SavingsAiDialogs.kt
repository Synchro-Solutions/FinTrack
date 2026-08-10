package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fintrack.proyecto4.ai.SavingsPlan
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.util.formatColones

@Composable
fun GenerateSavingsPlanDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onGeneratePlan: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Plan de ahorro con IA",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "FinTrack puede analizar tu meta y tus movimientos para crear un plan de ahorro personalizado.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = FinTrackColors.GreenPrimary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = "✨ El plan incluirá:",
                            color = FinTrackColors.GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "• Ahorro mensual recomendado",
                            color = colors.textPrimary
                        )

                        Text(
                            text = "• Categorías que podrías revisar",
                            color = colors.textPrimary
                        )

                        Text(
                            text = "• Una explicación personalizada",
                            color = colors.textPrimary
                        )
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = FinTrackColors.GreenPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "Analizando tu información financiera...",
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.textSecondary
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = FinTrackColors.ErrorColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = FinTrackColors.ErrorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGeneratePlan,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = FinTrackColors.White,
                    disabledContainerColor = colors.surfaceSecondary,
                    disabledContentColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isLoading) {
                        "Generando..."
                    } else {
                        "Generar plan"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isLoading
            ) {
                Text(
                    text = "Omitir y guardar",
                    color = colors.textSecondary
                )
            }
        }
    )
}

@Composable
fun SavingsPlanResultDialog(
    plan: SavingsPlan,
    onAccept: () -> Unit,
    onRegenerate: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.surface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "✨ Plan generado",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = FinTrackColors.GreenPrimary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ahorro mensual recomendado",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatColones(plan.monthlySaving.toLong()),
                            color = FinTrackColors.GreenPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "por mes",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (plan.categoriesToReduce.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Categorías que podrías revisar",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        plan.categoriesToReduce.forEach { category ->
                            Text(
                                text = "• $category",
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.divider
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "Recomendación",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = plan.explanation,
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = FinTrackColors.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Aceptar y guardar")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onRegenerate
                ) {
                    Text(
                        text = "Generar otro",
                        color = FinTrackColors.GreenPrimary
                    )
                }

                TextButton(
                    onClick = onCancel
                ) {
                    Text(
                        text = "Cancelar",
                        color = colors.textSecondary
                    )
                }
            }
        }
    )
}