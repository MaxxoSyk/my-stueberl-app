package de.landstueberl.mystueberlapp.data

/**
 * An event's products, prepared for the detail screen.
 *
 * Only AVAILABLE products are taken along: they make up [items] and the totals.
 * Products sold at this event's till are listed in [soldAtEvent]; anything sold
 * or removed elsewhere stays linked as history in [noLongerAvailable].
 */
data class EventProducts(
    /** Available products, grouped by batch, in assignment order. */
    val items: List<ProductListItem>,
    /** Sold at this event's till, in assignment order. */
    val soldAtEvent: List<Product>,
    /** Sold or removed elsewhere (e.g. at the stand before the event). */
    val noLongerAvailable: List<Product>,
    val availableCount: Int,
    val salesTotal: Money,
    val purchaseTotal: Money
) {
    val isEmpty: Boolean
        get() = items.isEmpty() && soldAtEvent.isEmpty() && noLongerAvailable.isEmpty()
}

/**
 * Pure, so it can be unit tested.
 *
 * @param eventSaleIds ids of this event's sales: a product counts as sold here
 *   only if its sale belongs to this event
 * @param newestFirst reverse the assignment order (while preparing, the newest
 *   product goes on top). Applied before batch grouping, so a batch moves up
 *   when a member is added.
 */
fun List<Product>.toEventProducts(
    eventSaleIds: Set<Int> = emptySet(),
    newestFirst: Boolean = false
): EventProducts {
    val ordered = if (newestFirst) asReversed() else this
    val (available, gone) = ordered.partition { it.details.status == ProductStatus.AVAILABLE }
    val (soldHere, elsewhere) = gone.partition { product ->
        product.details.eventSaleId?.let { it in eventSaleIds } == true
    }
    return EventProducts(
        items = available.groupByBatch(),
        soldAtEvent = soldHere,
        noLongerAvailable = elsewhere,
        availableCount = available.size,
        salesTotal = available.salesTotal(),
        purchaseTotal = available.sumMoney { it.details.purchasePrice }
    )
}

/** Sum of the sales prices, e.g. a basket's total. */
fun List<Product>.salesTotal(): Money = sumMoney { it.details.salesPrice }