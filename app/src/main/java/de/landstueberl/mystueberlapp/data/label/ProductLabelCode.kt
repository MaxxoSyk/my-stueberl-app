package de.landstueberl.mystueberlapp.data.label

/**
 * What a label's QR code identifies, always for one event: a single product,
 * or a whole batch (one label for identical items).
 *
 * Encoded as `STUEBERL:<version>:<eventId>:<productId>` or
 * `STUEBERL:<version>:<eventId>:B<batchId>`. Only capitals, digits and ':' are
 * used, so QR alphanumeric mode applies. The code stays at the smallest QR size
 * (21×21) while the content has at most 20 characters, e.g. event 99 with
 * product 999999 or batch 99999.
 */
sealed interface LabelCode {
    val eventId: Int
    fun encode(): String

    companion object {
        internal const val PREFIX = "STUEBERL"
        internal const val FORMAT_VERSION = 1
        internal const val BATCH_MARKER = "B"

        /**
         * Parses a scanned code, or returns null for anything that isn't one of
         * our labels (another stand's QR code, a URL, an unknown format version).
         */
        fun decode(raw: String): LabelCode? {
            val parts = raw.trim().split(':')
            if (parts.size != 4) return null
            if (parts[0] != PREFIX) return null
            if (parts[1].toIntOrNull() != FORMAT_VERSION) return null

            val eventId = parts[2].toPositiveIdOrNull() ?: return null
            val target = parts[3]
            return if (target.startsWith(BATCH_MARKER)) {
                target.removePrefix(BATCH_MARKER).toPositiveIdOrNull()
                    ?.let { BatchLabelCode(eventId = eventId, batchId = it) }
            } else {
                target.toPositiveIdOrNull()
                    ?.let { ProductLabelCode(eventId = eventId, productId = it) }
            }
        }

        /** Digits only, so "+5", "B" or "BB7" are rejected. IDs start at 1. */
        private fun String.toPositiveIdOrNull(): Int? =
            takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
    }
}

/** One label per product. */
data class ProductLabelCode(
    override val eventId: Int,
    val productId: Int
) : LabelCode {
    override fun encode(): String =
        "${LabelCode.PREFIX}:${LabelCode.FORMAT_VERSION}:$eventId:$productId"

    companion object {
        /**
         * Product codes only; a group label returns null. Kept for existing
         * callers, new code should use [LabelCode.decode].
         */
        fun decode(raw: String): ProductLabelCode? = LabelCode.decode(raw) as? ProductLabelCode
    }
}

/** One label for a whole batch: scanning it adds the next available member. */
data class BatchLabelCode(
    override val eventId: Int,
    val batchId: Int
) : LabelCode {
    override fun encode(): String =
        "${LabelCode.PREFIX}:${LabelCode.FORMAT_VERSION}:$eventId:${LabelCode.BATCH_MARKER}$batchId"
}