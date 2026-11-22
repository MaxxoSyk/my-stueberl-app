package de.landstueberl.mystueberlapp.data.model.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency

/**
 * Validation and parsing for monetary text input, shared by the product price
 * fields and the cost calculator's unit price field.
 *
 * Keeping both here means a validated string always parses, and every stored
 * amount is normalised the same way.
 */
object PriceInput {

    /**
     * Digits with at most two decimals. Both ',' (German) and '.' are accepted
     * as the decimal separator, so it doesn't matter which key the keyboard
     * offers. Prices are never negative.
     */
    private val PATTERN = Regex("^\\d*([.,]\\d{0,2})?$")

    /** Monetary amounts are stored with two decimal places. */
    private const val SCALE = 2

    /** The shop trades exclusively in euros. */
    val CURRENCY: Currency = Currency.getInstance("EUR")

    /** Scale-correct zero, for prices left blank. */
    val ZERO_AMOUNT: BigDecimal = BigDecimal.ZERO.setScale(SCALE)

    /** Whether [input] is acceptable as a partially typed price. */
    fun isValid(input: String): Boolean = input.isEmpty() || input.matches(PATTERN)

    /**
     * Parses [input] into a normalised amount, or null when it is blank or
     * unparseable (e.g. a lone separator). The comma is converted only here,
     * so the text field keeps showing what the user typed.
     */
    fun parseAmount(input: String): BigDecimal? =
        input.takeIf { it.isNotBlank() }
            ?.replace(',', '.')
            ?.toBigDecimalOrNull()
            ?.setScale(SCALE, RoundingMode.HALF_UP)

    /**
     * An amount as text for a price field, with the device's decimal separator:
     * "12,50" on a German phone, "12.50" on an English one. Always parseable
     * by [parseAmount]. Read each call, so a language change takes effect.
     */
    fun toInputText(amount: BigDecimal): String =
        amount.toPlainString().replace('.', DecimalFormatSymbols.getInstance().decimalSeparator)

    /** Parses [input], treating blank or unparseable as zero. */
    fun parseMoney(input: String): Money = Money(
        amount = parseAmount(input) ?: ZERO_AMOUNT,
        currency = CURRENCY
    )

    /** Zero in the shop's currency. */
    fun zeroMoney(): Money = Money(amount = ZERO_AMOUNT, currency = CURRENCY)
}