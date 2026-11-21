package de.landstueberl.mystueberlapp.data.model.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceInputTest {

    @Test
    fun `comma and dot are both accepted`() {
        assertTrue(PriceInput.isValid("12,5"))
        assertTrue(PriceInput.isValid("12.50"))
        assertFalse(PriceInput.isValid("12,5,0"))
        assertFalse(PriceInput.isValid("12,555"))
    }

    @Test
    fun `comma is parsed as decimal separator`() {
        assertEquals(PriceInput.parseAmount("12,5"), "12.50".toBigDecimal())
        assertEquals(PriceInput.parseAmount("12,"), "12.00".toBigDecimal())
        assertNull(PriceInput.parseAmount(","))
    }

    @Test
    fun `input text always parses back`() {
        val amount = "7.25".toBigDecimal()
        assertEquals(amount, PriceInput.parseAmount(PriceInput.toInputText(amount)))
    }
}