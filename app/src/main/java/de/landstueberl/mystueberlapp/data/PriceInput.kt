package de.landstueberl.mystueberlapp.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Validation and parsing for monetary text input, shared by the product price
 * fields and the cost calculator's unit price field.
 *
 * Keeping both here means a validated string always parses, and every stored
 * amount is normalised the same way.
 */
object PriceInput {

    /** Digits with at most two decimals. Prices are never negative. */
    private val PATTERN = Regex("^\\d*([.]\\d{0,2})?$")

    /** Monetary amounts are stored with two decimal places. */
    private const val SCALE = 2

    /**
     * Whether [input] is acceptable as a partially-typed price.
     *
     * An empty string is valid: the user has to be able to clear the field, and
     * an empty price means "not set".
     */
    fun isValid(input: String): Boolean = input.isEmpty() || input.matches(PATTERN)

    /**
     * Parses [input] into a normalised amount, or null when it is blank or
     * unparseable.
     *
     * The scale is fixed at [SCALE] so that stored amounts are directly
     * comparable — `BigDecimal.equals` is scale-sensitive, so without this `3.5`
     * and `3.50` would be treated as different values.
     */
    fun parseAmount(input: String): BigDecimal? =
        input.takeIf { it.isNotBlank() }
            ?.toBigDecimalOrNull()
            ?.setScale(SCALE, RoundingMode.HALF_UP)
}