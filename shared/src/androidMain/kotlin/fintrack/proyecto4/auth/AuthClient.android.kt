package fintrack.proyecto4.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

actual object AuthClient {
    actual suspend fun registerWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = Firebase.auth.createUserWithEmailAndPassword(email, password)
        result.user?.uid ?: error("No se pudo obtener uid")
    }

    actual suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = Firebase.auth.signInWithEmailAndPassword(email, password)
        result.user?.uid ?: error("No se pudo obtener uid")
    }

    actual suspend fun signOut() {
        Firebase.auth.signOut()
    }

    actual fun currentUserId(): String? = Firebase.auth.currentUser?.uid
    actual fun currentUserEmail(): String? = Firebase.auth.currentUser?.email
}

