package fintrack.proyecto4.profile

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Sube una foto de perfil o comprobante al preset "unsigned" de Cloudinary (ver
 * CloudinaryConfig). Al ser unsigned, no requiere la API secret: Cloudinary valida la subida
 * contra las reglas configuradas en el preset (carpeta, tamaño, formato) del lado del servidor.
 *
 * Usa OkHttp directamente (no el `formData {}` de Ktor): Ktor genera el `Content-Disposition`
 * del campo `name` sin comillas (`name=upload_preset`), y ese formato específico es rechazado
 * por el parser de Cloudinary con un error engañoso ("Upload preset must be specified"), aunque
 * el valor sí viaje correcto en el cuerpo. `MultipartBody.Builder` de OkHttp sí entrecomilla los
 * nombres como espera Cloudinary (igual que curl/los navegadores).
 */
object CloudinaryUploader {

    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uploadProfilePhoto(filePath: String): Result<String> =
        upload(filePath, CloudinaryConfig.UPLOAD_PRESET, "profile.jpg")

    suspend fun uploadReceiptPhoto(filePath: String): Result<String> =
        upload(filePath, CloudinaryConfig.RECEIPT_UPLOAD_PRESET, "receipt.jpg")

    private suspend fun upload(filePath: String, preset: String, fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", preset)
                    .addFormDataPart("file", fileName, File(filePath).asRequestBody("image/jpeg".toMediaType()))
                    .build()

                val request = Request.Builder()
                    .url(CloudinaryConfig.UPLOAD_URL)
                    .post(requestBody)
                    .build()

                http.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("Cloudinary respondió ${response.code}: $bodyText")
                    }

                    json.parseToJsonElement(bodyText).jsonObject["secure_url"]
                        ?.jsonPrimitive?.content
                        ?: error("Respuesta de Cloudinary sin secure_url")
                }
            }.onFailure { e -> Log.e("CloudinaryUploader", "Fallo al subir a preset '$preset'", e) }
        }
}
