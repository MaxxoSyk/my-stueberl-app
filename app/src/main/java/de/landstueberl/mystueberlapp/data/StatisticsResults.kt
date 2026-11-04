package de.landstueberl.mystueberlapp.data

/** Raw cent values for the period-dependent rows. */
data class PeriodStatistics(
    val revenueTotalCents: Long,
    val revenueSalesAreaCents: Long,
    val revenueOrdersCents: Long,
    val soldCount: Int
)

/** Raw cent values for the current year's fixed rows. */
data class YearStatistics(
    val revenueCents: Long,
    val costCents: Long,
    val highestSaleCents: Long,
    val highestProfitCents: Long,
    val soldCount: Int
)

data class AllTimeStatistics(
    val revenueCents: Long,
    val soldCount: Int
)