package fintrack.proyecto4.transaction

interface CustomCategoryRepository {
    suspend fun getCategories(uid: String): List<CustomCategory>

    /** Devuelve la categoría creada con el id generado por Firestore. */
    suspend fun addCategory(uid: String, category: CustomCategory): CustomCategory
    suspend fun updateCategory(uid: String, category: CustomCategory)
    suspend fun deleteCategory(uid: String, categoryId: String)
}

/** Implementación por defecto para plataformas sin Firestore (Web, tests). */
class NoOpCustomCategoryRepository : CustomCategoryRepository {
    override suspend fun getCategories(uid: String): List<CustomCategory> = emptyList()
    override suspend fun addCategory(uid: String, category: CustomCategory): CustomCategory = category
    override suspend fun updateCategory(uid: String, category: CustomCategory) = Unit
    override suspend fun deleteCategory(uid: String, categoryId: String) = Unit
}
