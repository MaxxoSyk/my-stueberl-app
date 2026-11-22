package de.landstueberl.mystueberlapp.data.model.order

import de.landstueberl.mystueberlapp.data.db.relation.Order
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.model.common.Money
import de.landstueberl.mystueberlapp.data.model.common.sumMoney

/** Sum of the order's product sales prices. Zero for an order with no products. */
val Order.total: Money
    get() = productList.orderTotal()

/** Sum of the products' sales prices. Zero for an empty list. */
fun List<Product>.orderTotal(): Money = sumMoney { it.details.salesPrice }