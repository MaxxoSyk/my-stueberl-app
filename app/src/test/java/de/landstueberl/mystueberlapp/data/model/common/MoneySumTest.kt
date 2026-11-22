package de.landstueberl.mystueberlapp.data.model.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneySumTest {

    private fun money(value: String) = Money(value.toBigDecimal(), PriceInput.CURRENCY)

    @Test
    fun `sums amounts with scale 2`() {
        val sum = listOf("12.50", "7.25", "0.25").sumMoney { money(it) }
        assertEquals(money("20.00"), sum)
    }

    @Test
    fun `empty list sums to zero`() {
        assertEquals(PriceInput.zeroMoney(), emptyList<String>().sumMoney { money(it) })
    }
}