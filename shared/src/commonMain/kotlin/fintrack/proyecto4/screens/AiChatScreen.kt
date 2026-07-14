package fintrack.proyecto4.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.ai.AiChatMessage
import fintrack.proyecto4.ai.AiChatViewModel
import fintrack.proyecto4.ai.ChatRole
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.screens.common.ScreenHeader
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.TransactionRepository

@Composable
fun AiChatScreen(
    transactionRepository: TransactionRepository,
    onBack: () -> Unit
) {
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = "ai_chat_$uid") {
        AiChatViewModel(transactionRepository, uid)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .imePadding()
    ) {
        ScreenHeader(
            title = "Asistente IA",
            onBack = onBack,
            trailingContent = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = Color.White, fontSize = 14.sp)
                }
            }
        )

        // Lista de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input
        ChatInputBar(
            isLoading = isLoading,
            onSend = { viewModel.sendMessage(it) }
        )
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val colors = LocalAppColors.current
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // Avatar del asistente
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) FinTrackColors.GreenPrimary.copy(alpha = 0.15f)
                    else colors.surface
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) FinTrackColors.GreenPrimary else colors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(colors.surfaceSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val colors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.linearGradient(
                        listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Color.White, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .background(colors.surface)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.textSecondary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    isLoading: Boolean,
    onSend: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    var text by remember { mutableStateOf("") }

    val canSend = text.isNotBlank() && !isLoading

    Surface(
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Pregúntame algo...",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = montserrat
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.bg,
                    unfocusedContainerColor = colors.bg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = FinTrackColors.GreenPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (canSend) { onSend(text); text = "" }
                }),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    fontFamily = montserrat
                )
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = if (canSend) Brush.linearGradient(
                            listOf(FinTrackColors.GreenDark, FinTrackColors.GreenPrimary)
                        ) else Brush.linearGradient(
                            listOf(
                                FinTrackColors.GreenDark.copy(alpha = 0.4f),
                                FinTrackColors.GreenPrimary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { if (canSend) { onSend(text); text = "" } },
                    enabled = canSend,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

