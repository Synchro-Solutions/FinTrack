package fintrack.proyecto4.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
private data class GroqResponse(
    val choices: List<GroqChoice> = emptyList()
)

@Serializable
private data class GroqChoice(
    val message: GroqMessage
)

class GroqClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun chat(
        systemPrompt: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): String {
        val messages = buildList {
            add(GroqMessage(role = "system", content = systemPrompt))
            history.forEach { msg ->
                add(GroqMessage(
                    role = if (msg.role == ChatRole.USER) "user" else "assistant",
                    content = msg.content
                ))
            }
            add(GroqMessage(role = "user", content = userMessage))
        }

        val request = GroqRequest(model = GroqConfig.MODEL, messages = messages)

        val httpResponse = http.post(GroqConfig.BASE_URL) {
            header(HttpHeaders.Authorization, "Bearer ${GroqConfig.API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val bodyText = httpResponse.bodyAsText()

        if (!httpResponse.status.isSuccess()) {
            val errorMsg = try {
                json.parseToJsonElement(bodyText).jsonObject["error"]
                    ?.jsonObject?.get("message")?.jsonPrimitive?.content
                    ?: "Error ${httpResponse.status.value}"
            } catch (e: Exception) {
                "Error ${httpResponse.status.value}: $bodyText"
            }
            throw Exception(errorMsg)
        }

        val response = json.decodeFromString(GroqResponse.serializer(), bodyText)
        return response.choices.firstOrNull()?.message?.content
            ?: "No pude generar una respuesta. Intenta de nuevo."
    }

    suspend fun listModels(): String {
        val httpResponse = http.get("https://api.groq.com/openai/v1/models") {
            header(HttpHeaders.Authorization, "Bearer ${GroqConfig.API_KEY}")
        }
        val bodyText = httpResponse.bodyAsText()
        return try {
            val obj = json.parseToJsonElement(bodyText).jsonObject
            val data = obj["data"]?.let {
                json.parseToJsonElement(it.toString())
            }
            bodyText
        } catch (e: Exception) {
            bodyText
        }
    }
}
