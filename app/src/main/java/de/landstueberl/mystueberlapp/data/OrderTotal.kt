package de.landstueberl.mystueberlapp.data

/** Sum of the order's product sales prices. Zero for an order with no products. */
val Order.total: Money
    get() = productList.orderTotal()

/** Sum of the products' sales prices. Zero for an empty list. */
fun List<Product>.orderTotal(): Money = Money(
    amount = fold(PriceInput.ZERO_AMOUNT) { sum, product ->
        sum + product.details.salesPrice.amount
    },
    currency = PriceInput.CURRENCY
)