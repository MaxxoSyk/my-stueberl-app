package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.EventSale

/** Header figures of the till. One sale = one customer. */
data class TillFigures(
    val revenue: Money,
    val itemsSold: Int,
    val customers: Int
)

/** Pure, so it can be unit tested. Uses the totals stored with each sale. */
fun List<EventSale>.toTillFigures(): TillFigures = TillFigures(
    revenue = Money(
        amount = fold(PriceInput.ZERO_AMOUNT) { acc, sale -> acc + sale.total.amount },
        currency = PriceInput.CURRENCY
    ),
    itemsSold = sumOf { it.itemCount },
    customers = size
)