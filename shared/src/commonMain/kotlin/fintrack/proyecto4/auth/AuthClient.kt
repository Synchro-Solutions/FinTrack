package fintrack.proyecto4.auth

expect object AuthClient {
    suspend fun registerWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signOut()
    fun currentUserId(): String?
}

