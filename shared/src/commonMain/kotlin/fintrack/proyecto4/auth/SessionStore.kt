package fintrack.proyecto4.auth

interface SessionStore {
    suspend fun getRememberMe(): Boolean
    suspend fun setRememberMe(value: Boolean)
    suspend fun getFailedAttempts(): Int
    suspend fun setFailedAttempts(value: Int)
    suspend fun getLockoutUntil(): Long
    suspend fun setLockoutUntil(value: Long)
    suspend fun clearFailedAttempts()
    suspend fun getAccessToken(): String?
    suspend fun setAccessToken(token: String?)
}
