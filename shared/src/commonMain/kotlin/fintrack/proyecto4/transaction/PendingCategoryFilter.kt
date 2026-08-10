package fintrack.proyecto4.transaction

/**
 * Traspasa un filtro de categoría hacia Movimientos al navegar desde otra pantalla (ej. al
 * tocar una categoría en el donut de Reportes), ya que Screen.Movimientos no lleva argumentos
 * propios. TransactionsScreen lo consume una sola vez al entrar (ver [consume]), así un
 * reingreso normal a la pantalla no arrastra un filtro viejo.
 */
object PendingCategoryFilter {
    private var pending: String? = null

    fun post(category: String) {
        pending = category
    }

    fun consume(): String? {
        val value = pending
        pending = null
        return value
    }
}
