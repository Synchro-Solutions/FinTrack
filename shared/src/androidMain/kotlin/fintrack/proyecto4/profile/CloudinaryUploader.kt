package fintrack.proyecto4.profile

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Sube una foto de perfil al preset "unsigned" de Cloudinary (ver CloudinaryConfig).
 * Al ser unsigned, no requiere la API secret: Cloudinary valida la subida contra las
 * reglas configuradas en el preset (carpeta, tamaño, formato) del lado del servidor.
 */
object CloudinaryUploader {

    private val http = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uploadProfilePhoto(filePath: String): Result<String> = runCatching {
        val bytes = File(filePath).readBytes()

        val response = http.submitFormWithBinaryData(
            url = CloudinaryConfig.UPLOAD_URL,
            formData = formData {
                append("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"profile.jpg\"")
                    }
                )
            }
        )

        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Cloudinary respondió ${response.status.value}: $bodyText")
        }

        json.parseToJsonElement(bodyText).jsonObject["secure_url"]
            ?.jsonPrimitive?.content
            ?: error("Respuesta de Cloudinary sin secure_url")
    }
}
