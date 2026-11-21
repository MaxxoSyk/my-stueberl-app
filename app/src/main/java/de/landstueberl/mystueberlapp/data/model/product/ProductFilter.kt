package de.landstueberl.mystueberlapp.data.model.product

import java.time.LocalDate

data class ProductFilter(
    val years: Set<Int> = setOf(LocalDate.now().year),
    val statuses: Set<ProductStatus> = setOf(ProductStatus.AVAILABLE),
    val productSourceFilter: ProductSourceFilter = ProductSourceFilter.FROM_SALES_AREA
)

enum class ProductSourceFilter {
    ALL,
    FROM_ORDER,
    FROM_EVENT,
    FROM_SALES_AREA
}