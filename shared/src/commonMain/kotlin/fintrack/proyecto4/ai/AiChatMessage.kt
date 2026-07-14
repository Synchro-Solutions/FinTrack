package fintrack.proyecto4.ai

enum class ChatRole { USER, MODEL }

data class AiChatMessage(
    val role: ChatRole,
    val content: String
)
