package de.landstueberl.mystueberlapp.data

/** A row in the till's basket: a single product, or all basket members of one batch. */
sealed interface BasketLine {
    val key: String

    data class Single(val product: Product) : BasketLine {
        override val key: String get() = "basket_p_${product.details.id}"
    }

    data class Batch(val batchId: Int, val products: List<Product>) : BasketLine {
        override val key: String get() = "basket_b_$batchId"
        val representative: Product get() = products.first()
        val count: Int get() = products.size
        val total: Money get() = products.sumMoney { it.details.salesPrice }
    }
}

/**
 * Groups the basket for display. Unlike [groupByBatch], a batch with one
 * member in the basket is still a batch row, so its counter can go up.
 * Keeps the order in which each line first appeared. Pure, so it can be unit tested.
 */
fun List<Product>.toBasketLines(): List<BasketLine> {
    val byBatch = filter { it.details.batchId != null }.groupBy { it.details.batchId!! }
    val emitted = mutableSetOf<Int>()
    return mapNotNull { product ->
        val batchId = product.details.batchId
        when {
            batchId == null -> BasketLine.Single(product)
            emitted.add(batchId) -> BasketLine.Batch(batchId, byBatch.getValue(batchId))
            else -> null
        }
    }
}