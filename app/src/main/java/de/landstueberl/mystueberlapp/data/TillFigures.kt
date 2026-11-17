package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.EventSale

/** Header figures of the till, plus the cost for the DONE summary. One sale = one customer. */
data class TillFigures(
    val revenue: Money,
    /** Purchase prices of everything sold, from the sale snapshots. */
    val purchaseCost: Money,
    val itemsSold: Int,
    val customers: Int
)

/** Pure, so it can be unit tested. Uses the totals stored with each sale. */
fun List<EventSale>.toTillFigures(): TillFigures = TillFigures(
    revenue = sumMoney { it.total },
    purchaseCost = sumMoney { it.purchaseTotal },
    // The standard sumOf for Int, no longer shadowed by the helper below.
    itemsSold = sumOf { it.itemCount },
    customers = size
)

/**
 * Every amount has scale 2, so the sum does too. Not named sumOf: that would
 * shadow the standard sumOf and break Int sums in this file.
 */
private inline fun List<EventSale>.sumMoney(selector: (EventSale) -> Money): Money =
    Money(
        amount = fold(PriceInput.ZERO_AMOUNT) { acc, sale -> acc + selector(sale).amount },
        currency = PriceInput.CURRENCY
    )