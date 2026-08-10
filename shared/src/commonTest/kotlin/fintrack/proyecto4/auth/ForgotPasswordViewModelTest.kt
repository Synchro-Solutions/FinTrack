package fintrack.proyecto4.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: ForgotPasswordViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = ForgotPasswordViewModel(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Estado inicial ───────────────────────────────────────────────────────

    @Test
    fun `estado inicial es Idle`() = runTest {
        assertIs<ForgotPasswordUiState.Idle>(viewModel.uiState.value)
    }

    // ─── Validación de email vacío ────────────────────────────────────────────

    @Test
    fun `email vacio muestra error inline sin llamar al repositorio`() = runTest {
        viewModel.sendResetLink("")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.passwordResetCallCount)
    }

    @Test
    fun `email en blanco muestra error inline sin llamar al repositorio`() = runTest {
        viewModel.sendResetLink("   ")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.passwordResetCallCount)
    }

    // ─── Validación de formato inválido ────────────────────────────────────────

    @Test
    fun `email con formato invalido muestra error inline sin llamar al repositorio`() = runTest {
        viewModel.sendResetLink("correo-sin-arroba")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.passwordResetCallCount)
    }

    @Test
    fun `email sin dominio muestra error inline`() = runTest {
        viewModel.sendResetLink("user@")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.passwordResetCallCount)
    }

    // ─── Envío exitoso / estado de confirmación ────────────────────────────────

    @Test
    fun `email valido llama al repositorio con email en minusculas y sin espacios`() = runTest {
        viewModel.sendResetLink("  USER@EMAIL.COM  ")

        assertEquals("user@email.com", fakeRepo.lastPasswordResetEmail)
    }

    @Test
    fun `envio exitoso cambia estado a Sent con confirmacion generica`() = runTest {
        fakeRepo.nextPasswordResetResult = PasswordResetResult.Success

        viewModel.sendResetLink("user@email.com")

        assertIs<ForgotPasswordUiState.Sent>(viewModel.uiState.value)
    }

    @Test
    fun `envio exitoso llama al repositorio exactamente una vez`() = runTest {
        viewModel.sendResetLink("user@email.com")

        assertEquals(1, fakeRepo.passwordResetCallCount)
    }

    // ─── Usuario inexistente sin revelar información ───────────────────────────

    @Test
    fun `usuario inexistente muestra la misma confirmacion generica que un envio real`() = runTest {
        // El repositorio ya colapsa "usuario no existe" en PasswordResetResult.Success
        // (ver FirebaseAuthRepository) — el ViewModel no debe distinguir el caso.
        fakeRepo.nextPasswordResetResult = PasswordResetResult.Success

        viewModel.sendResetLink("no-existe@email.com")

        val state = viewModel.uiState.value
        assertIs<ForgotPasswordUiState.Sent>(state)
        assertEquals(PASSWORD_RESET_COOLDOWN_SECONDS, state.secondsRemaining)
    }

    // ─── Error de conexión ──────────────────────────────────────────────────────

    @Test
    fun `error de conexion muestra mensaje sin revelar existencia del correo`() = runTest {
        fakeRepo.nextPasswordResetResult = PasswordResetResult.ConnectionError

        viewModel.sendResetLink("user@email.com")

        val state = viewModel.uiState.value
        assertIs<ForgotPasswordUiState.Error>(state)
        assertTrue(!state.message.contains("existe", ignoreCase = true))
    }

    // ─── Temporizador de 60 segundos ────────────────────────────────────────────

    @Test
    fun `el boton queda deshabilitado justo despues del envio con 60 segundos restantes`() = runTest {
        viewModel.sendResetLink("user@email.com")

        val state = viewModel.uiState.value
        assertIs<ForgotPasswordUiState.Sent>(state)
        assertEquals(60, state.secondsRemaining)
    }

    @Test
    fun `el contador baja hasta 0 y reactiva el boton`() = runTest {
        val observed = mutableListOf<Int>()
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect { state ->
                if (state is ForgotPasswordUiState.Sent) observed.add(state.secondsRemaining)
            }
        }

        viewModel.sendResetLink("user@email.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(60, observed.first())
        assertEquals(0, observed.last())
        assertEquals(observed.sortedDescending(), observed)

        collectJob.cancel()
    }

    // ─── Reset de error ─────────────────────────────────────────────────────────

    @Test
    fun `clearError vuelve a Idle desde Error`() = runTest {
        viewModel.sendResetLink("")
        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)

        viewModel.clearError()

        assertIs<ForgotPasswordUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `clearError no afecta el estado Sent`() = runTest {
        viewModel.sendResetLink("user@email.com")
        assertIs<ForgotPasswordUiState.Sent>(viewModel.uiState.value)

        viewModel.clearError()

        assertIs<ForgotPasswordUiState.Sent>(viewModel.uiState.value)
    }
}
