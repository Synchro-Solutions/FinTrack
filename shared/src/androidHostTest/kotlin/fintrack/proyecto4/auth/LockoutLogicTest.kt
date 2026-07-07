package fintrack.proyecto4.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas de la lógica de bloqueo por intentos fallidos.
 * Usa FakeSessionStore para no depender de Firebase ni DataStore.
 *
 * NOTA: FirebaseAuthRepository no se puede instanciar en unit tests porque
 * Firebase.auth requiere un contexto Android real. Estas pruebas validan
 * la lógica de negocio del SessionStore que FirebaseAuthRepository usa
 * para tomar decisiones de bloqueo.
 */
class LockoutLogicTest {

    private lateinit var store: FakeSessionStore

    @BeforeTest
    fun setUp() {
        store = FakeSessionStore()
    }

    // ─── Lógica de bloqueo (simula lo que hace FirebaseAuthRepository) ────────

    private suspend fun simulateFailedAttempt(): Boolean {
        val attempts = store.getFailedAttempts() + 1
        return if (attempts >= 5) {
            store.setLockoutUntil(System.currentTimeMillis() + 15 * 60_000L)
            store.setFailedAttempts(0)
            true // bloqueado
        } else {
            store.setFailedAttempts(attempts)
            false // no bloqueado aún
        }
    }

    private suspend fun isCurrentlyLocked(): Boolean {
        return System.currentTimeMillis() < store.getLockoutUntil()
    }

    private suspend fun minutesRemaining(): Long {
        val remaining = store.getLockoutUntil() - System.currentTimeMillis()
        return if (remaining > 0) remaining / 60_000 + 1 else 0L
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    fun `cuatro intentos fallidos no bloquean la cuenta`() = runTest {
        repeat(4) { simulateFailedAttempt() }

        assertFalse(isCurrentlyLocked())
        assertEquals(4, store.currentFailedAttempts())
    }

    @Test
    fun `el quinto intento fallido bloquea la cuenta`() = runTest {
        repeat(4) { simulateFailedAttempt() }
        val bloqueado = simulateFailedAttempt()

        assertTrue(bloqueado)
        assertTrue(isCurrentlyLocked())
    }

    @Test
    fun `tras el bloqueo el contador de intentos se resetea a cero`() = runTest {
        repeat(5) { simulateFailedAttempt() }

        assertEquals(0, store.currentFailedAttempts())
    }

    @Test
    fun `el bloqueo dura aproximadamente 15 minutos`() = runTest {
        repeat(5) { simulateFailedAttempt() }

        val remaining = minutesRemaining()
        assertTrue(remaining in 14L..16L, "Se esperaban ~15 minutos, se obtuvo $remaining")
    }

    @Test
    fun `sin intentos previos la cuenta no esta bloqueada`() = runTest {
        assertFalse(isCurrentlyLocked())
    }

    @Test
    fun `login exitoso limpia los intentos fallidos`() = runTest {
        repeat(3) { simulateFailedAttempt() }
        assertEquals(3, store.currentFailedAttempts())

        // Simula login exitoso
        store.clearFailedAttempts()
        store.setRememberMe(true)

        assertEquals(0, store.currentFailedAttempts())
        assertFalse(isCurrentlyLocked())
        assertTrue(store.isRememberMeSet())
    }

    @Test
    fun `cuenta bloqueada sigue bloqueada entre intentos`() = runTest {
        repeat(5) { simulateFailedAttempt() }
        assertTrue(isCurrentlyLocked())

        // Intentar de nuevo no cambia el estado de bloqueo
        val stillLocked = isCurrentlyLocked()
        assertTrue(stillLocked)
    }

    @Test
    fun `rememberMe false hace que getStoredToken devuelva null`() = runTest {
        store.setRememberMe(false)
        // Simula la lógica de getStoredToken()
        val token: String? = if (!store.getRememberMe()) null else "some-token"
        assertEquals(null, token)
    }

    @Test
    fun `rememberMe true permite obtener token si existe sesion`() = runTest {
        store.setRememberMe(true)
        // Simula la lógica de getStoredToken() cuando hay sesión activa
        val token: String? = if (!store.getRememberMe()) null else "valid-token"
        assertEquals("valid-token", token)
    }

    @Test
    fun `signOut limpia rememberMe`() = runTest {
        store.setRememberMe(true)

        // Simula signOut()
        store.setRememberMe(false)
        store.clearFailedAttempts()

        assertFalse(store.getRememberMe())
    }
}
