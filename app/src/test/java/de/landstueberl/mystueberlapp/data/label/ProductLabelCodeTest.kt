package de.landstueberl.mystueberlapp.data.label

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductLabelCodeTest {

    @Test
    fun `encode and decode round trip`() {
        val code = ProductLabelCode(eventId = 12, productId = 345)
        assertEquals("STUEBERL:1:12:345", code.encode())
        assertEquals(code, ProductLabelCode.decode(code.encode()))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals(ProductLabelCode.decode("  STUEBERL:1:3:7\n"), ProductLabelCode(3, 7))
    }

    @Test
    fun `foreign and malformed codes are rejected`() {
        listOf(
            "",
            "https://example.com",
            "OTHER:1:3:7",
            "STUEBERL:2:3:7",   // unknown format version
            "STUEBERL:1:3",     // missing product
            "STUEBERL:1:3:7:9", // extra part
            "STUEBERL:1:x:7",
            "STUEBERL:1:0:7",   // ids start at 1
            "STUEBERL:1:3:-7"
        ).forEach { raw ->
            assertNull("Expected null for '$raw'", ProductLabelCode.decode(raw))
        }
    }

    @Test
    fun `realistic ids fit the smallest QR size`() {
        // Version 1 (21 modules), level M, alphanumeric mode: 20 characters.
        // "STUEBERL:1::" takes 12, leaving 8 digits for both ids together.
        val content = ProductLabelCode(eventId = 99, productId = 999_999).encode()
        assertEquals(20, content.length)
        assertEquals(21, QrCodes.matrix(content).width)
    }

    @Test
    fun `very large ids grow the code by only one size`() {
        // Version 2 (25 modules): still small and easy to scan.
        val content = ProductLabelCode(eventId = 9999, productId = 99_999).encode()
        assertEquals(25, QrCodes.matrix(content).width)
    }
}