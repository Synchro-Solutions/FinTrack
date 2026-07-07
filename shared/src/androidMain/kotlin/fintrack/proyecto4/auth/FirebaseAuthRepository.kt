package fintrack.proyecto4.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.auth
import fintrack.proyecto4.database.DatabaseHelper

class FirebaseAuthRepository(
    private val sessionStore: SessionStore,
    private val databaseHelper: DatabaseHelper
) : AuthRepository {

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
            sessionStore.setAccessToken(token)
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

    override suspend fun register(email: String, password: String): RegisterResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val uid = result.user?.uid ?: throw Exception("ID de usuario no encontrado")
            val token = result.user?.getIdToken(false) ?: ""

            // Sincronización con base de datos local
            databaseHelper.queries.transaction {
                databaseHelper.queries.insertUser(uid, email, null)
                databaseHelper.queries.insertRole(uid, "USER")
                databaseHelper.queries.insertProfile(uid, "CRC")
            }
            
            sessionStore.setAccessToken(token)
            RegisterResult.Success(uid)
        } catch (e: FirebaseAuthException) {
            val message = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Este email ya está en uso"
                "ERROR_INVALID_EMAIL" -> "El formato del email es inválido"
                "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil"
                else -> e.message ?: "Error al registrar usuario"
            }
            RegisterResult.Error(message)
        } catch (e: Exception) {
            RegisterResult.Error(e.message ?: "Ocurrió un error inesperado")
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
        sessionStore.setAccessToken(null)
        sessionStore.clearFailedAttempts()
    }
}
