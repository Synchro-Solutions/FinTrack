package fintrack.proyecto4.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.auth

class FirebaseAuthRepository(private val sessionStore: SessionStore) : AuthRepository {

    private val auth = Firebase.auth

    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult {
        val lockoutUntil = sessionStore.getLockoutUntil()
        val now = System.currentTimeMillis()
        if (now < lockoutUntil) {
            val minutesRemaining = (lockoutUntil - now) / 60_000 + 1
            return LoginResult.AccountLocked(minutesRemaining)
        }

        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            val token = result.user?.getIdToken(false) ?: ""
            sessionStore.clearFailedAttempts()
            sessionStore.setRememberMe(rememberMe)
            LoginResult.Success(token)
        } catch (e: FirebaseAuthException) {
            val attempts = sessionStore.getFailedAttempts() + 1
            if (attempts >= 5) {
                sessionStore.setLockoutUntil(System.currentTimeMillis() + 15 * 60_000L)
                sessionStore.clearFailedAttempts()
                LoginResult.AccountLocked(15L)
            } else {
                sessionStore.setFailedAttempts(attempts)
                LoginResult.Error("Email o contraseña incorrectos")
            }
        } catch (e: Exception) {
            LoginResult.Error("Error de conexión. Intente de nuevo.")
        }
    }

    override suspend fun getStoredToken(): String? {
        if (!sessionStore.getRememberMe()) {
            auth.signOut()
            return null
        }
        return try {
            auth.currentUser?.getIdToken(false)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        sessionStore.setRememberMe(false)
        sessionStore.clearFailedAttempts()
    }
}
