package de.landstueberl.mystueberlapp.data.model.product

import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import de.landstueberl.mystueberlapp.data.model.common.Money
import de.landstueberl.mystueberlapp.data.model.common.PriceInput
import java.math.BigDecimal
import java.math.RoundingMode

/** Line total: quantity × unit price. */
val ProductMaterialDetail.lineTotal: BigDecimal
    get() = unitPrice.amount
        .multiply(BigDecimal(quantity))
        .setScale(2, RoundingMode.HALF_UP)

/**
 * Sum of all lines, or null when the list is empty — in which case the product
 * has no calculation and its purchase price was entered by hand.
 *
 * Folds the line totals directly instead of using sumMoney: lineTotal is a
 * BigDecimal (quantity × unit price), not a Money.
 */
fun List<ProductMaterialDetail>.calculatedTotal(): Money? {
    if (isEmpty()) return null
    return Money(
        amount = fold(PriceInput.ZERO_AMOUNT) { sum, line -> sum + line.lineTotal },
        currency = PriceInput.CURRENCY
    )
}