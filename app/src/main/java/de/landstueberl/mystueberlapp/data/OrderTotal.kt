package de.landstueberl.mystueberlapp.data

import java.math.BigDecimal

/**
 * Sum of the order's product sales prices. Products without a sales price count
 * as zero. Returns null when the order has no priced products at all, since
 * there is no currency to render.
 */
val Order.total: Money?
    get() {
        val currency = productList
            .firstNotNullOfOrNull { it.details.salesPrice?.currency }
            ?: return null

        val amount = productList.fold(BigDecimal.ZERO) { sum, product ->
            sum + (product.details.salesPrice?.amount ?: BigDecimal.ZERO)
        }

        return Money(amount = amount, currency = currency)
    }

/**
 * Sum of sales prices. Products without a sales price count as zero.
 * Null when no product has a price, since there is no currency to render.
 */
fun List<Product>.orderTotal(): Money? {
    val currency = firstNotNullOfOrNull { it.details.salesPrice?.currency }
        ?: return null

    val amount = fold(BigDecimal.ZERO) { sum, product ->
        sum + (product.details.salesPrice?.amount ?: BigDecimal.ZERO)
    }

    return Money(amount = amount, currency = currency)
}