package fintrack.proyecto4.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

/**
 * Conecta Auth y Firestore al Firebase Local Emulator Suite (carpeta firebase/, ver
 * `firebase emulators:start`) en vez de al proyecto real en la nube. Debe llamarse una
 * sola vez, antes de la primera lectura/escritura a Firebase.auth o Firebase.firestore
 * (por eso se invoca al inicio de MainActivity.onCreate, antes de construir los
 * repositorios). "10.0.2.2" es el loopback especial del emulador de Android hacia el
 * host de la máquina donde corren los emuladores de Firebase.
 */
object FirebaseEmulatorConfig {

    // Cambiar a true solo en desarrollo local con los emuladores corriendo.
    // Debe quedar en false antes de compilar un build para producción.
    const val USE_EMULATOR = false

    private const val EMULATOR_HOST = "10.0.2.2"
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080

    private var connected = false

    fun connectIfEnabled() {
        if (!USE_EMULATOR || connected) return
        Firebase.auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
        Firebase.firestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        connected = true
    }
}
