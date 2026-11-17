package de.landstueberl.mystueberlapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventResultTest {

    private fun money(value: String) = Money(value.toBigDecimal(), PriceInput.CURRENCY)

    private val figures = TillFigures(
        revenue = money("120.00"),
        purchaseCost = money("45.50"),
        itemsSold = 8,
        customers = 5
    )

    @Test
    fun `profit subtracts purchase cost and stand fee`() {
        val result = eventResult(figures, standFee = money("30.00"))
        assertEquals(money("44.50"), result.profit)
        assertTrue(result.includesStandFee)
    }

    @Test
    fun `without stand fee the profit only subtracts purchase cost`() {
        val result = eventResult(figures, standFee = null)
        assertEquals(money("74.50"), result.profit)
        assertFalse(result.includesStandFee)
    }

    @Test
    fun `profit can be negative`() {
        val result = eventResult(figures, standFee = money("100.00"))
        assertEquals(money("-25.50"), result.profit)
    }
}