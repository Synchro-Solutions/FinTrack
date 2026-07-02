package fintrack.proyecto4.onboarding

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Guarda y consulta el perfil de onboarding en Firestore.
 * Colecciones: users/{uid} (perfil) y users/{uid}/consents/onboarding (consentimientos).
 * Usa la estructura existente del proyecto, ya cubierta por las reglas de seguridad.
 */
class FirestoreOnboardingRepository : OnboardingRepository {

    private val db = Firebase.firestore

    override suspend fun saveProfile(uid: String, profile: UserProfile) {
        val userRef = db.collection("users").document(uid)
        userRef.set(
            mapOf(
                "name" to profile.name,
                "photoPath" to (profile.photoPath ?: ""),
                "income" to profile.income,
                "currency" to profile.currency,
                "onboardingComplete" to true
            )
        )
        userRef.collection("consents").document("onboarding").set(
            mapOf(
                "privacyAccepted" to profile.privacyAccepted,
                "termsAccepted" to profile.termsAccepted
            )
        )
    }

    override suspend fun isOnboardingComplete(uid: String): Boolean {
        return try {
            val doc = db.collection("users").document(uid).get()
            doc.exists && doc.get<Boolean>("onboardingComplete") == true
        } catch (e: Exception) {
            false
        }
    }
}
