package fintrack.proyecto4.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `estado inicial es Idle`() {
        assertIs<ForgotPasswordUiState.Idle>(viewModel.uiState.value)
        assertEquals(0, viewModel.cooldownSeconds.value)
    }

    // ─── Validación de formato (inline) ───────────────────────────────────────

    @Test
    fun `email vacio muestra error de validacion y no llama al repositorio`() = runTest {
        viewModel.sendResetLink("")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.sendPasswordResetCallCount)
    }

    @Test
    fun `email con formato invalido muestra error de validacion`() = runTest {
        viewModel.sendResetLink("correo-invalido")

        assertIs<ForgotPasswordUiState.Error>(viewModel.uiState.value)
        assertEquals(0, fakeRepo.sendPasswordResetCallCount)
    }

    @Test
    fun `isValidEmailFormat rechaza formatos invalidos y acepta validos`() {
        assertEquals(false, isValidEmailFormat(""))
        assertEquals(false, isValidEmailFormat("sin-arroba.com"))
        assertEquals(false, isValidEmailFormat("user@sin-dominio"))
        assertEquals(true, isValidEmailFormat("user@email.com"))
        assertEquals(true, isValidEmailFormat("  user@email.com  "))
    }

    // ─── Envío exitoso (mensaje genérico) ─────────────────────────────────────

    @Test
    fun `email valido cambia estado a Sent`() = runTest {
        viewModel.sendResetLink("user@email.com")

        assertIs<ForgotPasswordUiState.Sent>(viewModel.uiState.value)
    }

    @Test
    fun `envio exitoso llama al repositorio con email en minusculas y sin espacios`() = runTest {
        viewModel.sendResetLink("  USER@EMAIL.COM  ")

        assertEquals("user@email.com", fakeRepo.lastPasswordResetEmail)
    }

    @Test
    fun `email que no existe muestra el mismo estado Sent que uno que si existe`() = runTest {

        fakeRepo.nextPasswordResetResult = PasswordResetResult.Success

        viewModel.sendResetLink("no-existe@email.com")

        assertIs<ForgotPasswordUiState.Sent>(viewModel.uiState.value)
    }

    // ─── Error de conexión

    @Test
    fun `error de red cambia estado a Error con el mensaje del repositorio`() = runTest {
        fakeRepo.nextPasswordResetResult = PasswordResetResult.NetworkError("Error de conexión. Intente de nuevo.")

        viewModel.sendResetLink("user@email.com")

        val state = viewModel.uiState.value
        assertIs<ForgotPasswordUiState.Error>(state)
        assertEquals("Error de conexión. Intente de nuevo.", state.message)
    }

    // ─── Cooldown de 60 segundos (anti-spam) ──────────────────────────────────

    @Test
    fun `tras un envio exitoso el cooldown inicia en 60 segundos`() = runTest {
        viewModel.sendResetLink("user@email.com")

        assertEquals(PASSWORD_RESET_COOLDOWN_SECONDS, viewModel.cooldownSeconds.value)
    }

    @Test
    fun `un segundo intento durante el cooldown no vuelve a llamar al repositorio`() = runTest {
        viewModel.sendResetLink("user@email.com")
        assertEquals(1, fakeRepo.sendPasswordResetCallCount)

        viewModel.sendResetLink("user@email.com")

        assertEquals(1, fakeRepo.sendPasswordResetCallCount)
    }

    @Test
    fun `el cooldown llega a cero luego de 60 segundos`() = runTest {
        viewModel.sendResetLink("user@email.com")

        advanceTimeBy(60_000)

        assertEquals(0, viewModel.cooldownSeconds.value)
    }

    @Test
    fun `tras terminar el cooldown se puede volver a enviar`() = runTest {
        viewModel.sendResetLink("user@email.com")
        advanceTimeBy(60_000)

        viewModel.sendResetLink("user@email.com")

        assertEquals(2, fakeRepo.sendPasswordResetCallCount)
    }

    // ─── Limpieza de error al editar ──────────────────────────────────────────

    @Test
    fun `clearError vuelve a Idle desde Error`() = runTest {
        viewModel.sendResetLink("correo-invalido")
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
