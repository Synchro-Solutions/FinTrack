package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.screens.common.SuccessSnackbarHost
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.util.formatColones
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cuánto se muestra el Snackbar de éxito antes de navegar fuera de la pantalla. */
private const val SuccessSnackbarDelayMillis = 900L

/**
 * Ver y editar detalle de una transacción (US-14). La transacción llega completa desde la
 * navegación (ver Screen.TransactionDetail, poblado por TransactionsScreen), sin necesidad
 * de volver a pedirla a Firestore por id.
 */
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    onBack: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDeleted: () -> Unit
) {
    val uid = AuthClient.currentUserId() ?: ""
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    val accentColor = if (transaction.type == TransactionType.INCOME) {
        FinTrackColors.GreenPrimary
    } else {
        FinTrackColors.ErrorColor
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg)
    ) {
        ScreenHeader(
            title = "Detalle de movimiento",
            onBack = onBack,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp).clickable { onEdit(transaction) }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp)
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.type == TransactionType.INCOME) {
                            Icons.Default.TrendingUp
                        } else {
                            Icons.Default.TrendingDown
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${formatColones(transaction.amount)}",
                    color = accentColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = montserratFamily()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = transaction.description,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = montserratFamily()
                )
            }

            Spacer(Modifier.height(28.dp))

            DetailInfoCard {
                DetailRow(label = "Categoría", value = transaction.category)
                DetailDivider()
                DetailRow(
                    label = "Método de pago",
                    value = transaction.paymentMethod?.label ?: "No especificado"
                )
                DetailDivider()
                DetailRow(label = "Fecha", value = transaction.date)
                DetailDivider()
                DetailRow(
                    label = "Tipo",
                    value = if (transaction.type == TransactionType.INCOME) "Ingreso" else "Gasto"
                )
            }

            deleteError?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    color = FinTrackColors.ErrorColor,
                    fontSize = 12.sp,
                    fontFamily = montserratFamily()
                )
            }

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FinTrackColors.ErrorColor)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isDeleting) "Eliminando..." else "Eliminar movimiento",
                    fontWeight = FontWeight.Bold,
                    fontFamily = montserratFamily()
                )
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar movimiento") },
            text = { Text("Esta acción no se puede deshacer. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                transactionRepository.deleteTransaction(uid, transaction.id)
                                isDeleting = false
                                launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Movimiento eliminado exitosamente",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                delay(SuccessSnackbarDelayMillis)
                                onDeleted()
                            } catch (e: Exception) {
                                isDeleting = false
                                deleteError = "No se pudo eliminar. Intenta de nuevo."
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FinTrackColors.ErrorColor)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontFamily = montserratFamily()
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserratFamily()
        )
    }
}

@Composable
private fun DetailDivider() {
    val colors = LocalAppColors.current
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}
