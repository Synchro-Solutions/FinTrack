package fintrack.proyecto4.auth

suspend fun onRegisterClick(
    email: String,
    password: String,
    onSuccessNavigate: () -> Unit,
    onError: (String) -> Unit
) {
    if (email.isBlank() || password.length < 6) {
        onError("Email invalido o password muy corta (min 6)")
        return
    }

    AuthClient.registerWithEmail(email.trim(), password)
        .onSuccess { onSuccessNavigate() }
        .onFailure { onError(it.message ?: "Error al registrar") }
}

