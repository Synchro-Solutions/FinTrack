package fintrack.proyecto4.transaction

/** Categoría personalizada creada por el usuario, además del catálogo fijo de la app
 *  (ver [ExpenseCategories]/[IncomeCategories]). Sin color a propósito — no aplica al
 *  diseño actual de la app; el ícono es opcional. */
data class CustomCategory(
    val id: String = "",
    val name: String,
    val type: TransactionType,
    val icon: String? = null
)
