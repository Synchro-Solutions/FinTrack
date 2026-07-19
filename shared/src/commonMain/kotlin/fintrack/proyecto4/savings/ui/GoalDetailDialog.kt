package fintrack.proyecto4.savings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.FinTrackColors

@Composable
fun GoalDetailDialog(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    onDismiss: () -> Unit,
    onCancelGoal: (SavingsGoal) -> Unit,
    onEditGoal: (SavingsGoal) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "GoalDetailProgress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinTrackColors.SurfacePrimary,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${goal.iconName} ${goal.name}",
                    color = FinTrackColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailStatusBadge(
                        status = goal.status
                    )

                    DetailPriorityBadge(
                        priority = goal.priority
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailInformationCard(
                    label = "Categoría",
                    value = goal.categoryLabel
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso",
                            color = FinTrackColors.TextSecondary,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "${goal.progressPercentage}%",
                            color = FinTrackColors.GreenPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            animatedProgress
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (
                            goal.status ==
                            GoalStatus.CANCELLED
                        ) {
                            FinTrackColors.TextTertiary
                        } else {
                            FinTrackColors.GreenPrimary
                        },
                        trackColor =
                            FinTrackColors.SurfaceSecondary
                    )
                }

                GoalAmountsSummary(
                    goal = goal
                )

                HorizontalDivider(
                    color = FinTrackColors.DividerColor
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fecha y planificación",
                        color = FinTrackColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    DetailInformationCard(
                        label = "Fecha límite",
                        value = goal.deadline
                            ?: "Sin fecha definida"
                    )

                    DetailInformationCard(
                        label = "Tiempo restante",
                        value = goal.deadlineLabel,
                        valueColor = if (goal.isOverdue) {
                            FinTrackColors.ErrorColor
                        } else {
                            FinTrackColors.TextPrimary
                        }
                    )
                }

                if (
                    goal.status ==
                    GoalStatus.ACTIVE
                ) {
                    SavingsRecommendationSection(
                        monthlySaving =
                            goal.suggestedMonthlySaving,
                        weeklySaving =
                            goal.suggestedWeeklySaving
                    )
                }

                if (goal.notes.isNotBlank()) {
                    HorizontalDivider(
                        color = FinTrackColors.DividerColor
                    )

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notas",
                            color = FinTrackColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                FinTrackColors.SurfaceSecondary,
                            shape =
                                RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = goal.notes,
                                modifier =
                                    Modifier.padding(14.dp),
                                color =
                                    FinTrackColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = FinTrackColors.DividerColor
                )

                ContributionHistorySection(
                    contributions = contributions
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (
                    goal.status ==
                    GoalStatus.ACTIVE
                ) {
                    TextButton(
                        onClick = {
                            onEditGoal(goal)
                        }
                    ) {
                        Text(
                            text = "Editar",
                            color =
                                FinTrackColors.GreenPrimary
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            FinTrackColors.GreenPrimary,
                        contentColor =
                            FinTrackColors.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (
                goal.status ==
                GoalStatus.ACTIVE
            ) {
                TextButton(
                    onClick = {
                        onCancelGoal(goal)
                    }
                ) {
                    Text(
                        text = "Cancelar meta",
                        color = FinTrackColors.ErrorColor
                    )
                }
            }
        }
    )
}

@Composable
private fun GoalAmountsSummary(
    goal: SavingsGoal
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            DetailAmountCard(
                label = "Ahorrado",
                amount = goal.currentAmount,
                amountColor =
                    FinTrackColors.GreenPrimary,
                modifier = Modifier.weight(1f)
            )

            DetailAmountCard(
                label = "Objetivo",
                amount = goal.targetAmount,
                amountColor =
                    FinTrackColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        DetailAmountCard(
            label = "Monto restante",
            amount = goal.remainingAmount,
            amountColor = if (
                goal.remainingAmount <= 0
            ) {
                FinTrackColors.GreenPrimary
            } else {
                FinTrackColors.TextPrimary
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailAmountCard(
    label: String,
    amount: Double,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = FinTrackColors.SurfaceSecondary,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                color = FinTrackColors.TextSecondary,
                fontSize = 11.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = formatDetailMoney(amount),
                color = amountColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SavingsRecommendationSection(
    monthlySaving: Double?,
    weeklySaving: Double?
) {
    if (
        monthlySaving == null &&
        weeklySaving == null
    ) {
        return
    }

    HorizontalDivider(
        color = FinTrackColors.DividerColor
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Plan recomendado",
            color = FinTrackColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Para completar la meta dentro del plazo:",
            color = FinTrackColors.TextSecondary,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            monthlySaving?.let {
                RecommendationCard(
                    title = "Por mes",
                    amount = it,
                    modifier = Modifier.weight(1f)
                )
            }

            weeklySaving?.let {
                RecommendationCard(
                    title = "Por semana",
                    amount = it,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = FinTrackColors.GreenPrimary
            .copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                color = FinTrackColors.TextSecondary,
                fontSize = 11.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = formatDetailMoney(amount),
                color = FinTrackColors.GreenPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ContributionHistorySection(
    contributions: List<SavingsContribution>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de abonos",
                color = FinTrackColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${contributions.size} movimientos",
                color = FinTrackColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        if (contributions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FinTrackColors.SurfaceSecondary,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Aún no hay abonos registrados.",
                    modifier = Modifier.padding(14.dp),
                    color = FinTrackColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            contributions
                .take(10)
                .forEachIndexed { index, contribution ->
                    ContributionRow(
                        contribution = contribution
                    )

                    if (
                        index <
                        contributions.take(10).lastIndex
                    ) {
                        HorizontalDivider(
                            color =
                                FinTrackColors.DividerColor,
                            thickness = 0.5.dp
                        )
                    }
                }

            if (contributions.size > 10) {
                Text(
                    text = "Mostrando los 10 movimientos más recientes.",
                    color = FinTrackColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ContributionRow(
    contribution: SavingsContribution
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = FinTrackColors.GreenPrimary
                            .copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "＋",
                    color = FinTrackColors.GreenPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {
                Text(
                    text = "Abono",
                    color = FinTrackColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = contribution.createdAt,
                    color = FinTrackColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "+${formatDetailMoney(contribution.amount)}",
            color = FinTrackColors.GreenPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailInformationCard(
    label: String,
    value: String,
    valueColor: Color = FinTrackColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = FinTrackColors.TextSecondary,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailStatusBadge(
    status: GoalStatus
) {
    val text = when (status) {
        GoalStatus.ACTIVE -> "Activa"
        GoalStatus.COMPLETED -> "Completada"
        GoalStatus.CANCELLED -> "Cancelada"
    }

    val color = when (status) {
        GoalStatus.ACTIVE ->
            FinTrackColors.GreenPrimary

        GoalStatus.COMPLETED ->
            FinTrackColors.GreenLight

        GoalStatus.CANCELLED ->
            FinTrackColors.ErrorColor
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailPriorityBadge(
    priority: GoalPriority
) {
    val text = when (priority) {
        GoalPriority.LOW -> "Prioridad baja"
        GoalPriority.MEDIUM -> "Prioridad media"
        GoalPriority.HIGH -> "Prioridad alta"
    }

    val color = when (priority) {
        GoalPriority.LOW ->
            FinTrackColors.TextSecondary

        GoalPriority.MEDIUM ->
            FinTrackColors.WarningColor

        GoalPriority.HIGH ->
            FinTrackColors.ErrorColor
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDetailMoney(
    amount: Double
): String {
    val cleanAmount = amount.toLong()

    val formatted = cleanAmount
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return "₡$formatted"
}