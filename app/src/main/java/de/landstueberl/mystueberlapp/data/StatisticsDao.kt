package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Aggregate queries for the statistics screen.
 *
 * Kept separate from [ProductDao] and [OrderDao]: these read the same tables but
 * answer a different kind of question, and none of them is used by the CRUD
 * screens.
 *
 * Amounts are integer cents, so SUM and MAX are exact. Dates are epoch days, so
 * range comparisons are numeric.
 *
 * NOTE: status names are embedded as SQL string literals. Renaming a
 * [ProductStatus] constant will not break compilation — it will silently make
 * these queries match nothing.
 */
@Dao
interface StatisticsDao {

    /**
     * Figures for the selected period. Products with no sales price are excluded
     * from both the revenue and the count: an unpriced sale would contribute
     * nothing to revenue while inflating the count.
     */
    @Query("""
        SELECT
            COALESCE(SUM(sales_amount), 0) AS revenueTotalCents,
            COALESCE(SUM(CASE WHEN orderId IS NULL THEN sales_amount ELSE 0 END), 0)
                AS revenueSalesAreaCents,
            COALESCE(SUM(CASE WHEN orderId IS NOT NULL THEN sales_amount ELSE 0 END), 0)
                AS revenueOrdersCents,
            COUNT(*) AS soldCount
        FROM ProductDetail
        WHERE status = 'SOLD'
          AND sales_amount IS NOT NULL
          AND soldOn >= :from
          AND soldOn <= :to
    """)
    fun getPeriodStatisticsFlow(from: LocalDate, to: LocalDate): Flow<PeriodStatistics>

    /**
     * Figures for [from]..[to] that are not driven by the period selector.
     *
     * A missing purchase price counts as zero, so profit for such a product is
     * its full sales price.
     */
    @Query("""
        SELECT
            COALESCE(SUM(sales_amount), 0) AS revenueCents,
            COALESCE(SUM(COALESCE(purchase_amount, 0)), 0) AS costCents,
            COALESCE(MAX(sales_amount), 0) AS highestSaleCents,
            COALESCE(MAX(sales_amount - COALESCE(purchase_amount, 0)), 0)
                AS highestProfitCents,
            COUNT(*) AS soldCount
        FROM ProductDetail
        WHERE status = 'SOLD'
          AND sales_amount IS NOT NULL
          AND soldOn >= :from
          AND soldOn <= :to
    """)
    fun getYearStatisticsFlow(from: LocalDate, to: LocalDate): Flow<YearStatistics>

    /** Revenue and count over all sold products, for the all-time average. */
    @Query("""
        SELECT
            COALESCE(SUM(sales_amount), 0) AS revenueCents,
            COUNT(*) AS soldCount
        FROM ProductDetail
        WHERE status = 'SOLD' AND sales_amount IS NOT NULL
    """)
    fun getAllTimeStatisticsFlow(): Flow<AllTimeStatistics>

    /**
     * Days between creation and sale, averaged over all sold sales-area
     * products. Order products are excluded: their `createdAt` is when they were
     * made to order, not when they went on sale.
     */
    @Query("""
        SELECT AVG(soldOn - createdAt)
        FROM ProductDetail
        WHERE status = 'SOLD' AND orderId IS NULL AND soldOn IS NOT NULL
    """)
    fun getAverageStockPresenceFlow(): Flow<Double?>

    /** Products sold within [from]..[to] with no sales price. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE status = 'SOLD'
          AND sales_amount IS NULL
          AND soldOn >= :from
          AND soldOn <= :to
    """)
    fun getFreeProductCountFlow(from: LocalDate, to: LocalDate): Flow<Int>

    /** Products removed within [from]..[to]. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE status = 'REMOVED'
          AND removedOn >= :from
          AND removedOn <= :to
    """)
    fun getRemovedProductCountFlow(from: LocalDate, to: LocalDate): Flow<Int>
}