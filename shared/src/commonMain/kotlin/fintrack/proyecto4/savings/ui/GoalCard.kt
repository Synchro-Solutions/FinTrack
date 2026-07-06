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

@Composable
fun GoalCard(
    goal: SavingsGoal,
    onAddContribution: (SavingsGoal) -> Unit,
    onViewDetail: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = goal.status == GoalStatus.COMPLETED
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        label = "SavingsGoalProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF111C2E),
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
                        .background(Color(0xFF123D32), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = goal.iconName, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isCompleted) "Meta alcanzada" else "Faltan ${formatMoney(goal.remainingAmount)}",
                        color = if (isCompleted) Color(0xFF22C55E) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                    )

                    Text(
                        text = goal.deadlineLabel,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "${goal.progressPercentage}%",
                    color = Color(0xFF22C55E),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${formatMoney(goal.currentAmount)} / ${formatMoney(goal.targetAmount)}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFF263447)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onViewDetail(goal) }) {
                    Text("Detalle", color = Color(0xFF94A3B8))
                }

                if (!isCompleted) {
                    Button(
                        onClick = { onAddContribution(goal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
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