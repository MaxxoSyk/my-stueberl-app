package de.landstueberl.mystueberlapp.data

import java.time.LocalDate

data class ProductFilter(
    val years: Set<Int> = setOf(LocalDate.now().year),
    val statuses: Set<ProductStatus> = setOf(ProductStatus.AVAILABLE),
    val productSourceFilter: ProductSourceFilter = ProductSourceFilter.FROM_SALES_AREA
)

enum class ProductStatus {
    AVAILABLE,
    SOLD,
    REMOVED
}

enum class ProductSourceFilter {
    ALL,
    FROM_ORDER,
    FROM_SALES_AREA
}