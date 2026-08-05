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
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.ai.ProjectionStatus
import fintrack.proyecto4.ai.SavingsProjection

@Composable
fun GoalDetailDialog(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    projection: SavingsProjection?,
    isGeneratingProjection: Boolean,
    projectionError: String?,
    onGenerateProjection: () -> Unit,
    onDismiss: () -> Unit,
    onCancelGoal: (SavingsGoal) -> Unit,
    onEditGoal: (SavingsGoal) -> Unit
) {
    val colors = LocalAppColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "GoalDetailProgress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${goal.iconName} ${goal.name}",
                    color = colors.textPrimary,
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
                            color = colors.textSecondary,
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
                            colors.textSecondary
                        } else {
                            FinTrackColors.GreenPrimary
                        },
                        trackColor =
                            colors.surfaceSecondary
                    )
                }

                GoalAmountsSummary(
                    goal = goal
                )

                HorizontalDivider(
                    color = colors.divider
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fecha y planificación",
                        color = colors.textPrimary,
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
                            colors.textPrimary
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

                if (goal.hasAiPlan) {
                    AiSavingsPlanSection(goal)
                }

                if (
                    goal.status == GoalStatus.ACTIVE ||
                    goal.status == GoalStatus.COMPLETED
                ) {
                    SavingsProjectionSection(
                        projection = projection,
                        isGenerating = isGeneratingProjection,
                        errorMessage = projectionError,
                        onGenerateProjection = onGenerateProjection
                    )
                }

                if (goal.notes.isNotBlank()) {
                    HorizontalDivider(
                        color = colors.divider
                    )

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notas",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                colors.surfaceSecondary,
                            shape =
                                RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = goal.notes,
                                modifier =
                                    Modifier.padding(14.dp),
                                color =
                                    colors.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.divider
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
    val colors = LocalAppColors.current
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
                    colors.textPrimary,
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
                colors.textPrimary
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
    val colors = LocalAppColors.current
    Surface(
        modifier = modifier,
        color = colors.surfaceSecondary,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                color = colors.textSecondary,
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
    val colors = LocalAppColors.current
    if (
        monthlySaving == null &&
        weeklySaving == null
    ) {
        return
    }

    HorizontalDivider(
        color = colors.divider
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Plan recomendado",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Para completar la meta dentro del plazo:",
            color = colors.textSecondary,
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
    val colors = LocalAppColors.current
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
                color = colors.textSecondary,
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
    val colors = LocalAppColors.current
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
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${contributions.size} movimientos",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }

        if (contributions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surfaceSecondary,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Aún no hay abonos registrados.",
                    modifier = Modifier.padding(14.dp),
                    color = colors.textSecondary,
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
                                colors.divider,
                            thickness = 0.5.dp
                        )
                    }
                }

            if (contributions.size > 10) {
                Text(
                    text = "Mostrando los 10 movimientos más recientes.",
                    color = colors.textSecondary,
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
    val colors = LocalAppColors.current
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
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = contribution.createdAt,
                    color = colors.textSecondary,
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
    valueColor: Color? = null
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = valueColor ?: colors.textPrimary,
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
    val colors = LocalAppColors.current
    val text = when (priority) {
        GoalPriority.LOW -> "Prioridad baja"
        GoalPriority.MEDIUM -> "Prioridad media"
        GoalPriority.HIGH -> "Prioridad alta"
    }

    val color = when (priority) {
        GoalPriority.LOW ->
            colors.textSecondary

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

@Composable
private fun AiSavingsPlanSection(
    goal: SavingsGoal
) {
    val colors = LocalAppColors.current

    HorizontalDivider(
        color = colors.divider
    )

    Column(
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "🤖 Plan generado por IA",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        goal.aiMonthlySaving?.let {

            Surface(
                color = FinTrackColors.GreenPrimary
                    .copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier =
                        Modifier.padding(14.dp)
                ) {

                    Text(
                        text = "Ahorro mensual recomendado",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            formatDetailMoney(it),
                        color =
                            FinTrackColors.GreenPrimary,
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        if (
            goal.aiCategoriesToReduce.isNotEmpty()
        ) {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Categorías donde podrías ahorrar",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                goal.aiCategoriesToReduce.forEach {

                    Surface(
                        color =
                            colors.surfaceSecondary,
                        shape =
                            RoundedCornerShape(10.dp)
                    ) {

                        Text(
                            text = "• $it",
                            modifier =
                                Modifier.padding(10.dp),
                            color =
                                colors.textSecondary
                        )
                    }
                }
            }
        }

        if (
            goal.aiExplanation.isNotBlank()
        ) {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Explicación",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color =
                        colors.surfaceSecondary,
                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text =
                            goal.aiExplanation,
                        modifier =
                            Modifier.padding(14.dp),
                        color =
                            colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun SavingsProjectionSection(
    projection: SavingsProjection?,
    isGenerating: Boolean,
    errorMessage: String?,
    onGenerateProjection: () -> Unit
) {
    val colors = LocalAppColors.current

    HorizontalDivider(
        color = colors.divider
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "✨ Proyección inteligente",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Analiza tu ritmo de ahorro y estima cuándo podrías completar esta meta.",
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        if (projection == null) {
            Button(
                onClick = onGenerateProjection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTrackColors.GreenPrimary,
                    contentColor = FinTrackColors.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = FinTrackColors.White,
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Analizando...")
                } else {
                    Text("Generar proyección con IA")
                }
            }
        }

        errorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FinTrackColors.ErrorColor.copy(
                    alpha = 0.10f
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(14.dp),
                    color = FinTrackColors.ErrorColor,
                    fontSize = 12.sp
                )
            }
        }

        projection?.let { result ->
            ProjectionStatusCard(
                status = result.status
            )

            if (
                result.status != ProjectionStatus.COMPLETED &&
                result.status != ProjectionStatus.NO_DATA
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    ProjectionAmountCard(
                        label = "Promedio mensual",
                        amount = result.averageMonthlySaving,
                        modifier = Modifier.weight(1f)
                    )

                    ProjectionAmountCard(
                        label = "Necesario por mes",
                        amount = result.requiredMonthlySaving,
                        modifier = Modifier.weight(1f)
                    )
                }

                result.projectedCompletionDate?.let { date ->
                    DetailInformationCard(
                        label = "Finalización estimada",
                        value = date,
                        valueColor =
                            FinTrackColors.GreenPrimary
                    )
                }

                if (result.monthsRemaining > 0) {
                    DetailInformationCard(
                        label = "Meses disponibles",
                        value = result.monthsRemaining.toString()
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surfaceSecondary,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = result.explanation,
                    modifier = Modifier.padding(14.dp),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            TextButton(
                onClick = onGenerateProjection,
                modifier = Modifier.align(
                    Alignment.End
                ),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = FinTrackColors.GreenPrimary,
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )
                }

                Text(
                    text = if (isGenerating) {
                        "Actualizando..."
                    } else {
                        "Actualizar proyección"
                    },
                    color = FinTrackColors.GreenPrimary
                )
            }
        }
    }
}

@Composable
private fun ProjectionStatusCard(
    status: ProjectionStatus
) {
    val colors = LocalAppColors.current

    val title: String
    val description: String
    val statusColor: Color

    when (status) {
        ProjectionStatus.AHEAD -> {
            title = "Vas adelantado"
            description =
                "Tu ritmo de ahorro supera el necesario."
            statusColor =
                FinTrackColors.GreenPrimary
        }

        ProjectionStatus.ON_TRACK -> {
            title = "Vas por buen camino"
            description =
                "Tu ritmo actual es suficiente para cumplir la meta."
            statusColor =
                FinTrackColors.GreenPrimary
        }

        ProjectionStatus.BEHIND -> {
            title = "Necesitas mejorar el ritmo"
            description =
                "Tu ahorro mensual está por debajo de lo necesario."
            statusColor =
                FinTrackColors.WarningColor
        }

        ProjectionStatus.COMPLETED -> {
            title = "Meta completada"
            description =
                "Ya alcanzaste el monto definido."
            statusColor =
                FinTrackColors.GreenPrimary
        }

        ProjectionStatus.NO_DATA -> {
            title = "Datos insuficientes"
            description =
                "Registra más abonos para obtener una proyección precisa."
            statusColor =
                colors.textSecondary
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = statusColor.copy(
                            alpha = 0.15f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (status) {
                        ProjectionStatus.AHEAD -> "↗"
                        ProjectionStatus.ON_TRACK -> "✓"
                        ProjectionStatus.BEHIND -> "!"
                        ProjectionStatus.COMPLETED -> "★"
                        ProjectionStatus.NO_DATA -> "?"
                    },
                    color = statusColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {
                Text(
                    text = title,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ProjectionAmountCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier,
        color = colors.surfaceSecondary,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 11.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = formatDetailMoney(amount),
                color = FinTrackColors.GreenPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
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