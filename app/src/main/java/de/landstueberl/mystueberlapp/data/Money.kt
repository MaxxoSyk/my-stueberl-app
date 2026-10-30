package de.landstueberl.mystueberlapp.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

data class Money(
    val amount: BigDecimal,
    val currency: Currency
)

/**
 * Compares two amounts by numeric value rather than structurally.
 *
 * [BigDecimal.equals] is scale-sensitive — `3.5` and `3.50` are not equal — which
 * makes the generated [Money.equals] report a difference between two amounts that
 * are economically identical. Use this wherever "has the price changed?" is the
 * question.
 */
fun Money?.sameValueAs(other: Money?): Boolean = when {
    this == null && other == null -> true
    this == null || other == null -> false
    else -> currency == other.currency && amount.compareTo(other.amount) == 0
}

/**
 * The amount as plain decimal text, never in scientific notation.
 *
 * Amounts written since scale normalisation always have two decimals; older
 * rows may have fewer until they are next saved.
 */
fun Money.formatAmount(): String = amount.toPlainString()

/** The amount with its currency symbol, e.g. `12.50 €`. */
fun Money.format(): String =
    NumberFormat.getCurrencyInstance().apply {
        currency = this@format.currency
    }.format(amount)