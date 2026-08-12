package fintrack.proyecto4.ai

object GroqConfig {
    // La clave sale de local.properties (groq.apiKey=...) o de la variable de entorno
    // GROQ_API_KEY (ver shared/build.gradle.kts), nunca hardcodeada aca. Obtene la tuya
    // gratis en: https://console.groq.com → API Keys
    val API_KEY: String = GroqSecrets.API_KEY
    const val MODEL = "llama-3.1-8b-instant"
    const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
}
