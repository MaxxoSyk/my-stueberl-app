package de.landstueberl.mystueberlapp.data.label

/**
 * What a product label's QR code identifies: one product, for one event.
 *
 * Encoded as `STUEBERL:<version>:<eventId>:<productId>`. Only capitals,
 * digits and ':' are used, so QR alphanumeric mode applies. The code stays at
 * the smallest QR size (21×21) while both ids together have at most 8 digits
 * (e.g. event 99, product 999999); beyond that it grows one step to 25×25.
 */
data class ProductLabelCode(
    val eventId: Int,
    val productId: Int
) {
    fun encode(): String = "$PREFIX:$FORMAT_VERSION:$eventId:$productId"

    companion object {
        private const val PREFIX = "STUEBERL"
        private const val FORMAT_VERSION = 1

        /**
         * Parses a scanned code, or returns null for anything that isn't one of
         * our labels (another stand's QR code, a URL, an unknown format version).
         */
        fun decode(raw: String): ProductLabelCode? {
            val parts = raw.trim().split(':')
            if (parts.size != 4) return null
            if (parts[0] != PREFIX) return null
            if (parts[1].toIntOrNull() != FORMAT_VERSION) return null

            val eventId = parts[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val productId = parts[3].toIntOrNull()?.takeIf { it > 0 } ?: return null
            return ProductLabelCode(eventId = eventId, productId = productId)
        }
    }
}