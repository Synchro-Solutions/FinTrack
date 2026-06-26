package fintrack.proyecto4.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first

class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>
) : SessionStore {

    companion object {
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        private val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
    }

    override suspend fun getRememberMe(): Boolean =
        dataStore.data.first()[KEY_REMEMBER_ME] ?: false

    override suspend fun setRememberMe(value: Boolean) {
        dataStore.edit { it[KEY_REMEMBER_ME] = value }
    }

    override suspend fun getFailedAttempts(): Int =
        dataStore.data.first()[KEY_FAILED_ATTEMPTS] ?: 0

    override suspend fun setFailedAttempts(value: Int) {
        dataStore.edit { it[KEY_FAILED_ATTEMPTS] = value }
    }

    override suspend fun getLockoutUntil(): Long =
        dataStore.data.first()[KEY_LOCKOUT_UNTIL] ?: 0L

    override suspend fun setLockoutUntil(value: Long) {
        dataStore.edit { it[KEY_LOCKOUT_UNTIL] = value }
    }

    override suspend fun clearFailedAttempts() {
        dataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs.remove(KEY_LOCKOUT_UNTIL)
        }
    }
}
