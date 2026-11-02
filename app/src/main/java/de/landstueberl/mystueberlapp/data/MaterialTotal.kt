package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import java.math.BigDecimal

/** Line total: quantity × unit price. */
val ProductMaterialDetail.lineTotal: BigDecimal
    get() = unitPrice.amount.multiply(BigDecimal(quantity))

/**
 * Sum of all lines, or null when the list is empty — in which case the product
 * has no calculation and its purchase price was entered by hand.
 */
fun List<ProductMaterialDetail>.calculatedTotal(): Money? {
    if (isEmpty()) return null
    val currency = first().unitPrice.currency
    val amount = fold(BigDecimal.ZERO) { sum, line -> sum + line.lineTotal }
    return Money(amount = amount, currency = currency)
}