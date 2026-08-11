package fintrack.proyecto4.screens

import fintrack.proyecto4.navigation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialCenterMenuConfigTest {

    @Test
    fun `menu financiero contiene las 8 herramientas esperadas`() {
        val items = financialMenuItems()

        assertEquals(8, items.size)
        assertEquals(8, items.map { it.route }.distinct().size)
    }

    @Test
    fun `menu financiero incluye historial para badge y flujo de mas`() {
        val items = financialMenuItems()

        assertTrue(items.any { it.route == Screen.CalculationHistory })
    }

    @Test
    fun `menu financiero mantiene textos no vacios`() {
        val items = financialMenuItems()

        assertTrue(items.all { it.title.isNotBlank() })
        assertTrue(items.all { it.description.isNotBlank() })
    }
}

