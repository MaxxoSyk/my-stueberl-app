package de.landstueberl.mystueberlapp.data.label

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelCodeTest {

    @Test
    fun `batch code encode and decode round trip`() {
        val code = BatchLabelCode(eventId = 12, batchId = 7)
        assertEquals("STUEBERL:1:12:B7", code.encode())
        assertEquals(code, LabelCode.decode(code.encode()))
    }

    @Test
    fun `product code still decodes as product`() {
        assertEquals(LabelCode.decode("STUEBERL:1:12:345"), ProductLabelCode(12, 345))
    }

    @Test
    fun `old product decode ignores batch codes`() {
        assertNull(ProductLabelCode.decode("STUEBERL:1:12:B7"))
    }

    @Test
    fun `malformed batch codes are rejected`() {
        listOf(
            "STUEBERL:1:12:B",    // no batch id
            "STUEBERL:1:12:B0",   // ids start at 1
            "STUEBERL:1:12:BB7",
            "STUEBERL:1:12:B-7",
            "STUEBERL:1:12:b7",   // lower case isn't our format
            "STUEBERL:1:12:+7"
        ).forEach { raw ->
            assertNull("Expected null for '$raw'", LabelCode.decode(raw))
        }
    }

    @Test
    fun `realistic batch codes fit the smallest QR size`() {
        val content = BatchLabelCode(eventId = 99, batchId = 99_999).encode()
        assertEquals(20, content.length)
        assertEquals(21, QrCodes.matrix(content).width)
    }
}