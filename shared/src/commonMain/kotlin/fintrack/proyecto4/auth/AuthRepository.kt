package fintrack.proyecto4.auth

sealed class LoginResult {
    data class Success(val idToken: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class AccountLocked(val minutesRemaining: Long) : LoginResult()
}

sealed class PasswordResetResult {
    object Success : PasswordResetResult()
    data class NetworkError(val message: String) : PasswordResetResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult
    suspend fun signInWithGoogleIdToken(idToken: String): LoginResult

    suspend fun sendPasswordResetEmail(email: String): PasswordResetResult
    suspend fun getStoredToken(): String?
    suspend fun signOut()
}