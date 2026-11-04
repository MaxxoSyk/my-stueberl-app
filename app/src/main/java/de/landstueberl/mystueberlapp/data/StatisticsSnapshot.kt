package de.landstueberl.mystueberlapp.data

/** Figures that follow the period selector. */
data class PeriodFigures(
    val revenueTotal: Money,
    val revenueSalesArea: Money,
    val revenueOrders: Money,
    val soldCount: Int
)

/** Figures fixed to the current year or all time. */
data class OverallFigures(
    val profitThisYear: Money,
    val averageSalePriceThisYear: Money?,
    val averageSalePriceAllTime: Money?,
    val averageStockPresenceDays: Double?,
    val highestSalePriceThisYear: Money,
    val highestProfitThisYear: Money,
    val freeProductsThisYear: Int,
    val removedProductsThisYear: Int
)

/** Figures for the home screen's statistics tile. */
data class StatisticsTileFigures(
    val revenueToday: Money,
    val soldToday: Int,
    val revenueThisYear: Money
)