package de.landstueberl.mystueberlapp.data.label

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LabelIdentifiersTest {

    @Test
    fun `identifier has fixed length and no look-alikes`() {
        val id = LabelIdentifiers.generate(emptySet())
        assertEquals(LabelIdentifiers.LENGTH, id.length)
        assertTrue(id.none { it in "01OIL" })
    }

    @Test
    fun `taken identifiers are never returned`() {
        val taken = mutableSetOf<String>()
        repeat(500) { taken += LabelIdentifiers.generate(taken, Random(it)) }
        assertEquals(500, taken.size)
        assertFalse(LabelIdentifiers.generate(taken) in taken)
    }
}