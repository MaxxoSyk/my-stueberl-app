package de.landstueberl.mystueberlapp.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    init {
        require(amount.scale() == 2) {
            "Money amounts must have scale 2, was ${amount.scale()} ($amount)"
        }
    }
}

/** The amount with its currency symbol, e.g. `12.50 €`. */
fun Money.format(): String =
    NumberFormat.getCurrencyInstance().apply {
        currency = this@format.currency
    }.format(amount)

/**
 * Sum of money values, e.g. a basket total or an event's revenue.
 *
 * Every stored amount has scale 2 and the fold starts at [PriceInput.ZERO_AMOUNT],
 * so the sum has scale 2 too and satisfies Money's check. An empty list sums to zero.
 * The shop trades only in euros, so the result is always [PriceInput.CURRENCY].
 *
 * Not named sumOf: that would shadow the standard sumOf and break Int sums.
 */
inline fun <T> Iterable<T>.sumMoney(selector: (T) -> Money): Money =
    Money(
        amount = fold(PriceInput.ZERO_AMOUNT) { acc, item -> acc + selector(item).amount },
        currency = PriceInput.CURRENCY
    )