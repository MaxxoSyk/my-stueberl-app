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
     * Figures for the selected period. Every sold product counts: prices are
     * non-nullable, so a product given away is one sold at zero.
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
          AND soldOn >= :from
          AND soldOn <= :to
    """)
    fun getPeriodStatisticsFlow(from: LocalDate, to: LocalDate): Flow<PeriodStatistics>

    /** Figures for [from]..[to] that are not driven by the period selector. */
    @Query("""
        SELECT
            COALESCE(SUM(sales_amount), 0) AS revenueCents,
            COALESCE(MAX(sales_amount), 0) AS highestSaleCents,
            COALESCE(MAX(sales_amount - purchase_amount), 0) AS highestProfitCents,
            COUNT(*) AS soldCount
        FROM ProductDetail
        WHERE status = 'SOLD'
          AND soldOn >= :from
          AND soldOn <= :to
    """)
    fun getYearStatisticsFlow(from: LocalDate, to: LocalDate): Flow<YearStatistics>

    /**
     * Profit for [from]..[to]: revenue from products sold, minus the cost of
     * every product that left stock — sold **or** removed.
     *
     * A removed product earned nothing but still cost something to make, so its
     * purchase price reduces profit. Cost is recognised when a product's fate is
     * decided, dated by `soldOn` or `removedOn` accordingly, which matches the
     * basis used by the rest of the screen.
     */
    @Query("""
        SELECT
            COALESCE(SUM(
                CASE WHEN status = 'SOLD' THEN sales_amount ELSE 0 END
            ), 0) AS revenueCents,
            COALESCE(SUM(purchase_amount), 0) AS costCents
        FROM ProductDetail
        WHERE (status = 'SOLD' AND soldOn >= :from AND soldOn <= :to)
           OR (status = 'REMOVED' AND removedOn >= :from AND removedOn <= :to)
    """)
    fun getProfitStatisticsFlow(from: LocalDate, to: LocalDate): Flow<ProfitStatistics>

    /** Revenue and count over all sold products, for the all-time average. */
    @Query("""
        SELECT
            COALESCE(SUM(sales_amount), 0) AS revenueCents,
            COUNT(*) AS soldCount
        FROM ProductDetail
        WHERE status = 'SOLD'
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

    /** Products sold at no charge within [from]..[to]. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE status = 'SOLD'
          AND sales_amount = 0
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