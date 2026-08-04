package fintrack.proyecto4.auth

sealed class LoginResult {
    data class Success(val idToken: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class AccountLocked(val minutesRemaining: Long) : LoginResult()
}

/**
 * Resultado de solicitar el correo de recuperación de contraseña (US-04).
 * No existe un caso "usuario no encontrado": por diseño, ese error de Firebase
 * se trata igual que [Success] para no revelar si un correo está registrado.
 */
sealed class PasswordResetResult {
    data object Success : PasswordResetResult()
    data object ConnectionError : PasswordResetResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult
    suspend fun signInWithGoogleIdToken(idToken: String): LoginResult
    suspend fun sendPasswordResetEmail(email: String): PasswordResetResult
    suspend fun getStoredToken(): String?
    suspend fun signOut()
}
