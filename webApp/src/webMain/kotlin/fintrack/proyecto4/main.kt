package fintrack.proyecto4

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import fintrack.proyecto4.auth.FirebaseAuthRepository
import fintrack.proyecto4.auth.JsSessionStore
import fintrack.proyecto4.auth.requestGoogleIdToken
import fintrack.proyecto4.config.EnvConfig

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Firebase.initialize(
        context = null,
        options = FirebaseOptions(
            applicationId = EnvConfig.FIREBASE_APP_ID,
            apiKey = EnvConfig.FIREBASE_API_KEY,
            projectId = EnvConfig.FIREBASE_PROJECT_ID,
            authDomain = EnvConfig.FIREBASE_AUTH_DOMAIN,
            storageBucket = EnvConfig.FIREBASE_STORAGE_BUCKET,
            gcmSenderId = EnvConfig.FIREBASE_GCM_SENDER_ID
        )
    )

    ComposeViewport {
        val sessionStore = JsSessionStore()
        val authRepository = FirebaseAuthRepository(sessionStore)
        App(
            authRepository = authRepository,
            onGoogleSignInRequested = { requestGoogleIdToken(EnvConfig.GOOGLE_WEB_CLIENT_ID) }
        )
    }
}