package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.relation.Order
import de.landstueberl.mystueberlapp.data.db.relation.Product

/** Sum of the order's product sales prices. Zero for an order with no products. */
val Order.total: Money
    get() = productList.orderTotal()

/** Sum of the products' sales prices. Zero for an empty list. */
fun List<Product>.orderTotal(): Money = sumMoney { it.details.salesPrice }