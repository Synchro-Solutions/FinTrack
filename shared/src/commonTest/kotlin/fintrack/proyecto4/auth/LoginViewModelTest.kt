package fintrack.proyecto4.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = LoginViewModel(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Estado inicial ───────────────────────────────────────────────────────

    @Test
    fun `estado inicial es Idle`() {
        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    // ─── Validaciones de campos vacíos ────────────────────────────────────────

    @Test
    fun `signIn con email vacio no cambia el estado`() = runTest {
        viewModel.signIn(email = "", password = "password123", rememberMe = false)

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.signInCallCount)
    }

    @Test
    fun `signIn con password vacio no cambia el estado`() = runTest {
        viewModel.signIn(email = "user@email.com", password = "", rememberMe = false)

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.signInCallCount)
    }

    @Test
    fun `signIn con email y password en blanco no cambia el estado`() = runTest {
        viewModel.signIn(email = "   ", password = "   ", rememberMe = false)

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.signInCallCount)
    }

    // ─── Login exitoso ────────────────────────────────────────────────────────

    @Test
    fun `signIn exitoso cambia estado a Success`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Success("token-abc")

        viewModel.signIn("user@email.com", "password123", rememberMe = false)

        assertIs<LoginUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `signIn llama al repositorio con email en minusculas`() = runTest {
        viewModel.signIn("USER@EMAIL.COM", "password123", rememberMe = false)

        assertEquals("user@email.com", fakeRepo.lastEmail)
    }

    @Test
    fun `signIn llama al repositorio con email sin espacios`() = runTest {
        viewModel.signIn("  user@email.com  ", "password123", rememberMe = false)

        assertEquals("user@email.com", fakeRepo.lastEmail)
    }

    @Test
    fun `signIn exitoso pasa rememberMe correcto al repositorio`() = runTest {
        viewModel.signIn("user@email.com", "password123", rememberMe = true)

        assertEquals(true, fakeRepo.lastRememberMe)
    }

    @Test
    fun `signIn llama al repositorio exactamente una vez`() = runTest {
        viewModel.signIn("user@email.com", "password123", rememberMe = false)

        assertEquals(1, fakeRepo.signInCallCount)
    }

    // ─── Login fallido ────────────────────────────────────────────────────────

    @Test
    fun `credenciales incorrectas cambian estado a Error`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Error("Email o contraseña incorrectos")

        viewModel.signIn("user@email.com", "wrong", rememberMe = false)

        assertIs<LoginUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `mensaje de error se propaga correctamente al estado`() = runTest {
        val expectedMessage = "Email o contraseña incorrectos"
        fakeRepo.nextSignInResult = LoginResult.Error(expectedMessage)

        viewModel.signIn("user@email.com", "wrong", rememberMe = false)

        val state = viewModel.uiState.value
        assertIs<LoginUiState.Error>(state)
        assertEquals(expectedMessage, state.message)
    }

    // ─── Cuenta bloqueada ─────────────────────────────────────────────────────

    @Test
    fun `cuenta bloqueada cambia estado a Locked`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.AccountLocked(minutesRemaining = 15L)

        viewModel.signIn("user@email.com", "wrong", rememberMe = false)

        assertIs<LoginUiState.Locked>(viewModel.uiState.value)
    }

    @Test
    fun `minutos restantes se propagan correctamente al estado Locked`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.AccountLocked(minutesRemaining = 13L)

        viewModel.signIn("user@email.com", "wrong", rememberMe = false)

        val state = viewModel.uiState.value
        assertIs<LoginUiState.Locked>(state)
        assertEquals(13L, state.minutesRemaining)
    }

    // ─── Reset de estado ──────────────────────────────────────────────────────

    @Test
    fun `resetState vuelve a Idle desde Error`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Error("error")
        viewModel.signIn("user@email.com", "wrong", rememberMe = false)
        assertIs<LoginUiState.Error>(viewModel.uiState.value)

        viewModel.resetState()

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `resetState vuelve a Idle desde Locked`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.AccountLocked(15L)
        viewModel.signIn("user@email.com", "wrong", rememberMe = false)
        assertIs<LoginUiState.Locked>(viewModel.uiState.value)

        viewModel.resetState()

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `resetState vuelve a Idle desde Success`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Success("token")
        viewModel.signIn("user@email.com", "password", rememberMe = false)
        assertIs<LoginUiState.Success>(viewModel.uiState.value)

        viewModel.resetState()

        assertIs<LoginUiState.Idle>(viewModel.uiState.value)
    }

    // ─── Múltiples intentos consecutivos ─────────────────────────────────────

    @Test
    fun `varios intentos fallidos acumulan llamadas al repositorio`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Error("error")

        repeat(3) {
            viewModel.resetState()
            viewModel.signIn("user@email.com", "wrong$it", rememberMe = false)
        }

        assertEquals(3, fakeRepo.signInCallCount)
    }

    @Test
    fun `intento exitoso tras varios fallidos cambia estado a Success`() = runTest {
        fakeRepo.nextSignInResult = LoginResult.Error("error")
        viewModel.signIn("user@email.com", "wrong", rememberMe = false)
        viewModel.resetState()

        fakeRepo.nextSignInResult = LoginResult.Success("token")
        viewModel.signIn("user@email.com", "correct", rememberMe = false)

        assertIs<LoginUiState.Success>(viewModel.uiState.value)
    }
}
