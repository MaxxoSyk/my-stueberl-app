package de.landstueberl.mystueberlapp.data.converter

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

class MoneyConverters {

    /**
     * Amounts are stored as integer **cents**, not text.
     *
     * TEXT storage meant `SUM` needed `CAST(... AS REAL)`, which accumulates
     * floating-point error on money, and `ORDER BY` sorted lexicographically —
     * `"100.00"` before `"9.00"`. INTEGER cents makes both exact.
     *
     * The Kotlin API stays [BigDecimal]: the conversion happens only at the
     * database boundary.
     */
    @TypeConverter
    fun bigDecimalToCents(value: BigDecimal?): Long? =
        value?.movePointRight(CENTS_SHIFT)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()

    @TypeConverter
    fun centsToBigDecimal(value: Long?): BigDecimal? =
        value?.let { BigDecimal.valueOf(it, CENTS_SHIFT) }

    @TypeConverter
    fun currencyToString(currency: Currency?): String? =
        currency?.currencyCode

    @TypeConverter
    fun stringToCurrency(code: String?): Currency? =
        code?.let { Currency.getInstance(it) }

    private companion object {
        /** Two decimal places. */
        const val CENTS_SHIFT = 2
    }
}