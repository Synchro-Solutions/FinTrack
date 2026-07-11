package fintrack.proyecto4.auth

private fun unsupportedResult(): Result<String> = Result.failure(
    UnsupportedOperationException("Firebase Auth no configurado en target JS para este proyecto")
)

actual object AuthClient {
    actual suspend fun registerWithEmail(email: String, password: String): Result<String> = unsupportedResult()

    actual suspend fun signInWithEmail(email: String, password: String): Result<String> = unsupportedResult()

    actual suspend fun signOut() = Unit

    actual fun currentUserId(): String? = null
    actual fun currentUserEmail(): String? = null
}

