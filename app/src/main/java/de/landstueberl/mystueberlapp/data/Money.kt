package de.landstueberl.mystueberlapp.data

import java.math.BigDecimal
import java.util.Currency

data class Money(
    val amount: BigDecimal,
    val currency: Currency
)