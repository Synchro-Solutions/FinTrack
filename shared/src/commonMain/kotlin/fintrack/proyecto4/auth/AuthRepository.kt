package fintrack.proyecto4.auth

sealed class LoginResult {
    data class Success(val idToken: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class AccountLocked(val minutesRemaining: Long) : LoginResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult
    suspend fun getStoredToken(): String?
    suspend fun signOut()
}
