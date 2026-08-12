package fintrack.proyecto4.history

interface CalculationHistoryRepository {
    suspend fun getCalculations(uid: String): List<SavedCalculation>
    suspend fun saveCalculation(uid: String, calculation: SavedCalculation)
    suspend fun deleteCalculation(uid: String, calculationId: String)
}

class NoOpCalculationHistoryRepository : CalculationHistoryRepository {
    override suspend fun getCalculations(uid: String): List<SavedCalculation> = emptyList()
    override suspend fun saveCalculation(uid: String, calculation: SavedCalculation) = Unit
    override suspend fun deleteCalculation(uid: String, calculationId: String) = Unit
}
