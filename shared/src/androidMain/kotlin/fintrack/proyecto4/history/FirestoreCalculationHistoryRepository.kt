package fintrack.proyecto4.history

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Persiste cálculos guardados en users/{uid}/financialCalculations/{calculationId}. La colección
 * ya existe reservada en las reglas de seguridad de Firestore (solo permisos de dueño, sin
 * validación de forma), mismo patrón que FirestoreTransactionRepository/FirestoreBudgetRepository.
 */
class FirestoreCalculationHistoryRepository : CalculationHistoryRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("financialCalculations")

    override suspend fun getCalculations(uid: String): List<SavedCalculation> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    SavedCalculation(
                        id = doc.id,
                        tipo = CalculationType.valueOf(doc.get<String>("tipo")),
                        fechaCalculo = doc.get<Long?>("fechaCalculo") ?: 0L,
                        resumen = doc.get<String?>("resumen") ?: "",
                        montoPrincipal = doc.get<Long?>("montoPrincipal"),
                        detalle = doc.get<Map<String, String>?>("detalle") ?: emptyMap()
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveCalculation(uid: String, calculation: SavedCalculation) {
        val ref = if (calculation.id.isBlank()) col(uid).document else col(uid).document(calculation.id)
        ref.set(toMap(calculation))
    }

    override suspend fun deleteCalculation(uid: String, calculationId: String) {
        col(uid).document(calculationId).delete()
    }

    private fun toMap(calculation: SavedCalculation) = mapOf(
        "tipo" to calculation.tipo.name,
        "fechaCalculo" to calculation.fechaCalculo,
        "resumen" to calculation.resumen,
        "montoPrincipal" to calculation.montoPrincipal,
        "detalle" to calculation.detalle
    )
}
