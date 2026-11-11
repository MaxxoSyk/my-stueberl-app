package de.landstueberl.mystueberlapp.data

/**
 * An event's products, prepared for the detail screen.
 *
 * Only AVAILABLE products are taken along: they make up [items] and the totals.
 * Products sold or removed before the event stay linked (as history) but are
 * listed separately in [noLongerAvailable].
 */
data class EventProducts(
    /** Available products, grouped by batch, in assignment order. */
    val items: List<ProductListItem>,
    /** Sold or removed before the event, in assignment order. */
    val noLongerAvailable: List<Product>,
    val availableCount: Int,
    val salesTotal: Money,
    val purchaseTotal: Money
) {
    val isEmpty: Boolean
        get() = items.isEmpty() && noLongerAvailable.isEmpty()
}

/** Pure, so it can be unit tested. Keeps the receiver's (assignment) order. */
fun List<Product>.toEventProducts(): EventProducts {
    val (available, gone) = partition { it.details.status == ProductStatus.AVAILABLE }
    return EventProducts(
        items = available.groupByBatch(),
        noLongerAvailable = gone,
        availableCount = available.size,
        salesTotal = available.sumMoney { it.details.salesPrice },
        purchaseTotal = available.sumMoney { it.details.purchasePrice }
    )
}

/** Every amount has scale 2, so the sum does too and satisfies Money's check. */
private inline fun List<Product>.sumMoney(selector: (Product) -> Money): Money =
    Money(
        amount = fold(PriceInput.ZERO_AMOUNT) { acc, product -> acc + selector(product).amount },
        currency = PriceInput.CURRENCY
    )