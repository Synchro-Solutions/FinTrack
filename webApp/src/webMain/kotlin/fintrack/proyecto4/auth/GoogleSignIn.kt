package fintrack.proyecto4.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private external val google: dynamic
private external val document: dynamic

private const val GIS_SCRIPT_SRC = "https://accounts.google.com/gsi/client"

/**
 * Carga bajo demanda el script de Google Identity Services (GIS) la primera vez que se
 * necesita, en vez de bloquear el arranque de la app con un <script> en index.html.
 */
private suspend fun ensureGoogleIdentityServicesLoaded() {
    val alreadyLoaded = js("typeof google !== 'undefined' && typeof google.accounts !== 'undefined'") as Boolean
    if (alreadyLoaded) return

    suspendCancellableCoroutine { continuation ->
        val script = document.createElement("script")
        script.src = GIS_SCRIPT_SRC
        script.async = true
        script.onload = { if (continuation.isActive) continuation.resume(Unit) }
        script.onerror = {
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(IllegalStateException("No se pudo cargar Google Identity Services")))
            }
        }
        document.head.appendChild(script)
    }
}

/**
 * Equivalente web de MainActivity.requestGoogleIdToken (Android usa Credential Manager;
 * el navegador no lo tiene, así que aquí se usa Google Identity Services). [clientId] debe
 * ser el mismo Web client ID de Firebase (Authentication > Sign-in method > Google) que usa
 * androidApp vía R.string.google_web_client_id, para que ambos emitan tokens para el mismo
 * proyecto de Firebase.
 */
suspend fun requestGoogleIdToken(clientId: String): Result<String> = try {
    ensureGoogleIdentityServicesLoaded()
    suspendCancellableCoroutine { continuation ->
        val config = js("({})")
        config.client_id = clientId
        config.callback = { response: dynamic ->
            val idToken = response.credential as? String
            if (continuation.isActive) {
                continuation.resume(
                    if (idToken != null) {
                        Result.success(idToken)
                    } else {
                        Result.failure(IllegalStateException("Respuesta de Google sin credencial"))
                    }
                )
            }
        }
        google.accounts.id.initialize(config)

        google.accounts.id.prompt { notification: dynamic ->
            val cancelled = (notification.isNotDisplayed() as Boolean) || (notification.isSkippedMoment() as Boolean)
            if (cancelled && continuation.isActive) {
                continuation.resume(Result.failure(IllegalStateException("Inicio de sesión con Google cancelado")))
            }
        }
    }
} catch (e: Exception) {
    Result.failure(e)
}
