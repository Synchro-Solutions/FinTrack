package fintrack.proyecto4.auth

sealed class LoginResult {
    data class Success(val idToken: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class AccountLocked(val minutesRemaining: Long) : LoginResult()
}

sealed class RegisterResult {
    data class Success(val uid: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult
    suspend fun register(email: String, password: String): RegisterResult
    suspend fun getStoredToken(): String?
    suspend fun signOut()
}
