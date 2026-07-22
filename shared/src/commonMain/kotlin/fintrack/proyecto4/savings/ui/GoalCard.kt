package fintrack.proyecto4.savings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.savings.model.GoalColor
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors

@Composable
fun GoalCard(
    goal: SavingsGoal,
    onAddContribution: (SavingsGoal) -> Unit,
    onViewDetail: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val isCompleted =
        goal.status == GoalStatus.COMPLETED

    val isCancelled =
        goal.status == GoalStatus.CANCELLED

    val accentColor = goalAccentColor(goal.colorName)

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "SavingsGoalProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            accentColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = goal.iconName,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = goal.name,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = goal.categoryLabel,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                GoalStatusBadge(
                    status = goal.status
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) {
                        "Meta alcanzada"
                    } else if (isCancelled) {
                        "Meta cancelada"
                    } else {
                        "Faltan ${formatGoalMoney(goal.remainingAmount)}"
                    },
                    color = when {
                        isCompleted ->
                            FinTrackColors.GreenPrimary

                        isCancelled ->
                            FinTrackColors.ErrorColor

                        else ->
                            colors.textSecondary
                    },
                    fontSize = 13.sp,
                    fontWeight = if (
                        isCompleted || isCancelled
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )

                Text(
                    text = "${goal.progressPercentage}%",
                    color = if (isCancelled) {
                        colors.textSecondary
                    } else {
                        FinTrackColors.GreenPrimary
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ahorrado",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatGoalMoney(
                            goal.currentAmount
                        ),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {
                    Text(
                        text = "Objetivo",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatGoalMoney(
                            goal.targetAmount
                        ),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = {
                    animatedProgress
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    isCancelled ->
                        colors.textSecondary

                    else ->
                        FinTrackColors.GreenPrimary
                },
                trackColor =
                    colors.surfaceSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = goal.deadlineLabel,
                        color = if (goal.isOverdue) {
                            FinTrackColors.ErrorColor
                        } else {
                            colors.textSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = if (goal.isOverdue) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )

                    PriorityBadge(
                        priority = goal.priority
                    )
                }

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            onViewDetail(goal)
                        }
                    ) {
                        Text(
                            text = "Detalle",
                            color =
                                FinTrackColors.GreenPrimary
                        )
                    }

                    if (
                        goal.status ==
                        GoalStatus.ACTIVE
                    ) {
                        Button(
                            onClick = {
                                onAddContribution(goal)
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        FinTrackColors.GreenPrimary,
                                    contentColor =
                                        FinTrackColors.White
                                ),
                            shape =
                                RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Abonar",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStatusBadge(
    status: GoalStatus
) {
    val label = when (status) {
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
            .clip(RoundedCornerShape(50))
            .background(
                color.copy(alpha = 0.14f)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PriorityBadge(
    priority: GoalPriority
) {
    val colors = LocalAppColors.current
    val label = when (priority) {
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
            .clip(RoundedCornerShape(50))
            .background(
                color.copy(alpha = 0.12f)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun goalAccentColor(
    color: GoalColor
): Color {
    return when (color) {
        GoalColor.GREEN ->
            FinTrackColors.GreenPrimary

        GoalColor.BLUE ->
            FinTrackColors.BlueMeta

        GoalColor.ORANGE ->
            FinTrackColors.WarningColor

        GoalColor.PURPLE ->
            FinTrackColors.VioletLight

        GoalColor.RED ->
            FinTrackColors.ErrorColor
    }
}

private fun formatGoalMoney(
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