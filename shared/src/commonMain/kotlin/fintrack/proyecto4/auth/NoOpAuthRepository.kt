package fintrack.proyecto4.auth

class NoOpAuthRepository : AuthRepository {
    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult =
        LoginResult.Error("Login not available on web yet")
    override suspend fun getStoredToken(): String? = null
    override suspend fun signOut() = Unit
}