package fintrack.proyecto4.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas unitarias de FakeSessionStore.
 * Valida que la lógica de estado del SessionStore funcione correctamente,
 * lo cual garantiza que los tests que usan FakeSessionStore son confiables.
 */
class FakeSessionStoreTest {

    private lateinit var store: FakeSessionStore

    @BeforeTest
    fun setUp() {
        store = FakeSessionStore()
    }

    // ─── Estado inicial ───────────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene rememberMe en false`() = runTest {
        assertFalse(store.getRememberMe())
    }

    @Test
    fun `estado inicial tiene intentos fallidos en cero`() = runTest {
        assertEquals(0, store.getFailedAttempts())
    }

    @Test
    fun `estado inicial tiene lockoutUntil en cero`() = runTest {
        assertEquals(0L, store.getLockoutUntil())
    }

    // ─── RememberMe ───────────────────────────────────────────────────────────

    @Test
    fun `setRememberMe en true persiste correctamente`() = runTest {
        store.setRememberMe(true)
        assertTrue(store.getRememberMe())
    }

    @Test
    fun `setRememberMe en false persiste correctamente`() = runTest {
        store.setRememberMe(true)
        store.setRememberMe(false)
        assertFalse(store.getRememberMe())
    }

    // ─── Intentos fallidos ────────────────────────────────────────────────────

    @Test
    fun `setFailedAttempts persiste el valor correctamente`() = runTest {
        store.setFailedAttempts(3)
        assertEquals(3, store.getFailedAttempts())
    }

    @Test
    fun `incrementar intentos fallidos acumula correctamente`() = runTest {
        repeat(4) { i ->
            store.setFailedAttempts(store.getFailedAttempts() + 1)
            assertEquals(i + 1, store.getFailedAttempts())
        }
    }

    // ─── Bloqueo temporal ────────────────────────────────────────────────────

    @Test
    fun `setLockoutUntil persiste el timestamp correctamente`() = runTest {
        val timestamp = System.currentTimeMillis() + 15 * 60_000L
        store.setLockoutUntil(timestamp)
        assertEquals(timestamp, store.getLockoutUntil())
    }

    // ─── clearFailedAttempts ──────────────────────────────────────────────────

    @Test
    fun `clearFailedAttempts resetea el contador a cero`() = runTest {
        store.setFailedAttempts(4)
        store.clearFailedAttempts()
        assertEquals(0, store.getFailedAttempts())
    }

    @Test
    fun `clearFailedAttempts resetea el lockoutUntil a cero`() = runTest {
        store.setLockoutUntil(System.currentTimeMillis() + 60_000L)
        store.clearFailedAttempts()
        assertEquals(0L, store.getLockoutUntil())
    }

    @Test
    fun `clearFailedAttempts no afecta rememberMe`() = runTest {
        store.setRememberMe(true)
        store.setFailedAttempts(3)
        store.clearFailedAttempts()
        assertTrue(store.getRememberMe())
    }

    // ─── reset general ────────────────────────────────────────────────────────

    @Test
    fun `reset vuelve todos los valores al estado inicial`() = runTest {
        store.setRememberMe(true)
        store.setFailedAttempts(5)
        store.setLockoutUntil(System.currentTimeMillis() + 60_000L)

        store.reset()

        assertFalse(store.getRememberMe())
        assertEquals(0, store.getFailedAttempts())
        assertEquals(0L, store.getLockoutUntil())
    }
}
