package fintrack.proyecto4.savings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isCompleted = goal.status == GoalStatus.COMPLETED
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "SavingsGoalProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(FinTrackColors.GreenPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = goal.iconName, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isCompleted) "Meta alcanzada" else "Faltan ${formatMoney(goal.remainingAmount)}",
                        color = if (isCompleted) FinTrackColors.GreenPrimary else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                    )

                    Text(
                        text = goal.deadlineLabel,
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "${goal.progressPercentage}%",
                    color = FinTrackColors.GreenPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${formatMoney(goal.currentAmount)} / ${formatMoney(goal.targetAmount)}",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = FinTrackColors.GreenPrimary,
                trackColor = colors.surfaceSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onViewDetail(goal) }) {
                    Text("Detalle", color = colors.textSecondary)
                }

                if (!isCompleted) {
                    Button(
                        onClick = { onAddContribution(goal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinTrackColors.GreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Abonar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatMoney(amount: Double): String {
    val cleanAmount = amount.toInt()
    val formatted = cleanAmount.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return "₡$formatted"
}