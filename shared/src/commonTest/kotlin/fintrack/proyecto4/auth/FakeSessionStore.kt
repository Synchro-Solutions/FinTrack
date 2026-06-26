package fintrack.proyecto4.auth

/**
 * Implementación en memoria de SessionStore para pruebas unitarias.
 * No toca disco — cada instancia parte limpia.
 */
class FakeSessionStore : SessionStore {

    private var rememberMe = false
    private var failedAttempts = 0
    private var lockoutUntil = 0L

    override suspend fun getRememberMe(): Boolean = rememberMe
    override suspend fun setRememberMe(value: Boolean) { rememberMe = value }

    override suspend fun getFailedAttempts(): Int = failedAttempts
    override suspend fun setFailedAttempts(value: Int) { failedAttempts = value }

    override suspend fun getLockoutUntil(): Long = lockoutUntil
    override suspend fun setLockoutUntil(value: Long) { lockoutUntil = value }

    override suspend fun clearFailedAttempts() {
        failedAttempts = 0
        lockoutUntil = 0L
    }

    // Helpers para verificar estado en los tests
    fun currentFailedAttempts() = failedAttempts
    fun currentLockoutUntil() = lockoutUntil
    fun isRememberMeSet() = rememberMe

    fun reset() {
        rememberMe = false
        failedAttempts = 0
        lockoutUntil = 0L
    }
}
