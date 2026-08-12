package fintrack.proyecto4.onboarding

data class UserProfile(
    val name: String,
    val photoPath: String?,
    val income: Double,
    val currency: String,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val budgetAlertEnabled: Boolean = true
)

interface OnboardingRepository {
    suspend fun saveProfile(uid: String, profile: UserProfile)
    suspend fun isOnboardingComplete(uid: String): Boolean
    suspend fun getProfile(uid: String): UserProfile?
}

/** Implementación por defecto para plataformas sin Firestore (Web, tests). */
class NoOpOnboardingRepository : OnboardingRepository {
    override suspend fun saveProfile(uid: String, profile: UserProfile) = Unit
    override suspend fun isOnboardingComplete(uid: String): Boolean = true
    override suspend fun getProfile(uid: String): UserProfile? = null
}
