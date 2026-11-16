package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.EventSale

/** A completed sale with the products it contained. */
data class SaleWithProducts(
    val sale: EventSale,
    val products: List<Product>
)

/** Pure, so it can be unit tested. Keeps the order of [sales] (newest first). */
fun groupSales(sales: List<EventSale>, soldProducts: List<Product>): List<SaleWithProducts> {
    val bySale = soldProducts.groupBy { it.details.eventSaleId }
    return sales.map { SaleWithProducts(it, bySale[it.id].orEmpty()) }
}