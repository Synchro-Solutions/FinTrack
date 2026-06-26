package fintrack.proyecto4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.preferences.preferencesDataStore
import fintrack.proyecto4.auth.DataStoreSessionStore
import fintrack.proyecto4.auth.FirebaseAuthRepository

private val ComponentActivity.dataStore by preferencesDataStore(name = "fintrack_session")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sessionStore = DataStoreSessionStore(dataStore)
        val authRepository = FirebaseAuthRepository(sessionStore)

        setContent {
            App(authRepository = authRepository)
        }
    }
}
