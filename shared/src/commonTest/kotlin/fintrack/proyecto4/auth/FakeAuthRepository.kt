package fintrack.proyecto4.auth

/**
 * Implementación falsa de AuthRepository para pruebas unitarias.
 * Permite configurar qué resultado devuelve cada llamada a signIn.
 */
class FakeAuthRepository : AuthRepository {

    // Resultado que devolverá el próximo signIn — configurable por cada test
    var nextSignInResult: LoginResult = LoginResult.Success("fake-token")

    // Token que devolverá getStoredToken — null simula que no hay sesión activa
    var storedToken: String? = null

    // Registro de llamadas para verificar que se llamaron los métodos correctos
    var signInCallCount = 0
    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastRememberMe: Boolean? = null
    var signOutCalled = false

    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): LoginResult {
        signInCallCount++
        lastEmail = email
        lastPassword = password
        lastRememberMe = rememberMe
        return nextSignInResult
    }

    override suspend fun getStoredToken(): String? = storedToken

    override suspend fun signOut() {
        signOutCalled = true
        storedToken = null
    }

    fun reset() {
        nextSignInResult = LoginResult.Success("fake-token")
        storedToken = null
        signInCallCount = 0
        lastEmail = null
        lastPassword = null
        lastRememberMe = null
        signOutCalled = false
    }
}
