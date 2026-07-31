package fintrack.proyecto4

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import fintrack.proyecto4.auth.FirebaseAuthRepository
import fintrack.proyecto4.auth.JsSessionStore

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Firebase.initialize(
        context = null,
        options = FirebaseOptions(
            applicationId = "1:852790158784:web:729be6dc457b8d1c229425",
            apiKey = "AIzaSyCIzypZsOuWNTjlRCtjuFyUs1WLpYfspOQ",
            projectId = "fintrack-bm0911",
            authDomain = "fintrack-bm0911.firebaseapp.com",
            storageBucket = "fintrack-bm0911.firebasestorage.app",
            gcmSenderId = "852790158784"
        )
    )

    ComposeViewport {
        val sessionStore = JsSessionStore()
        val authRepository = FirebaseAuthRepository(sessionStore)
        App(authRepository = authRepository)
    }
}