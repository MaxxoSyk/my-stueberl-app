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