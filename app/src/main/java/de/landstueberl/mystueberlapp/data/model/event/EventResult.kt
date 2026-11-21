package de.landstueberl.mystueberlapp.data.model.event

import de.landstueberl.mystueberlapp.data.model.common.Money
import de.landstueberl.mystueberlapp.data.model.common.PriceInput
import de.landstueberl.mystueberlapp.data.model.till.TillFigures

/** A finished event's figures with its real profit. */
data class EventResult(
    val revenue: Money,
    val purchaseCost: Money,
    /** Null = not entered; profit is then calculated without it. */
    val standFee: Money?,
    val profit: Money,
    val itemsSold: Int,
    val customers: Int
) {
    val includesStandFee: Boolean
        get() = standFee != null
}

/**
 * Profit = revenue − purchase cost of the sold products − stand fee.
 * Pure, so it can be unit tested. All amounts have scale 2, so the result does too.
 */
fun eventResult(figures: TillFigures, standFee: Money?): EventResult {
    val fee = standFee?.amount ?: PriceInput.ZERO_AMOUNT
    return EventResult(
        revenue = figures.revenue,
        purchaseCost = figures.purchaseCost,
        standFee = standFee,
        profit = Money(
            amount = figures.revenue.amount - figures.purchaseCost.amount - fee,
            currency = PriceInput.CURRENCY
        ),
        itemsSold = figures.itemsSold,
        customers = figures.customers
    )
}