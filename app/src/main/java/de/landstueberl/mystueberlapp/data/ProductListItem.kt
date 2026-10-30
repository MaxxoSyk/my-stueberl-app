package de.landstueberl.mystueberlapp.data

/**
 * A row in the products list: either a single product, or a batch of identical
 * ones shown collapsed under a count.
 */
sealed interface ProductListItem {

    /** Stable key for LazyColumn, unique across both kinds. */
    val key: String

    data class Single(val product: Product) : ProductListItem {
        override val key: String get() = "product_${product.details.id}"
    }

    data class Batch(
        val batchId: Int,
        val products: List<Product>
    ) : ProductListItem {
        override val key: String get() = "batch_$batchId"

        /** The product whose details represent the group. */
        val representative: Product get() = products.first()

        val count: Int get() = products.size
    }
}

/**
 * Collapses products sharing a `batchId` into [ProductListItem.Batch] rows,
 * preserving the order of the receiver.
 *
 * A batch with only one visible member renders as a normal row — a group of one
 * is just a product. Products with no `batchId` are always individual.
 */
fun List<Product>.groupByBatch(): List<ProductListItem> {
    val byBatch = this
        .mapNotNull { product -> product.details.batchId?.let { it to product } }
        .groupBy({ it.first }, { it.second })

    val result = mutableListOf<ProductListItem>()
    val emitted = mutableSetOf<Int>()

    for (product in this) {
        val batchId = product.details.batchId
        val members = batchId?.let { byBatch[it] }

        when {
            // Not batched, or the last one left in its batch
            members == null || members.size == 1 ->
                result += ProductListItem.Single(product)

            // Already represented by a group row further up
            batchId in emitted -> Unit

            else -> {
                emitted += batchId
                result += ProductListItem.Batch(batchId = batchId, products = members)
            }
        }
    }

    return result
}