package de.landstueberl.mystueberlapp.repository

import de.landstueberl.mystueberlapp.data.AllTimeStatistics
import de.landstueberl.mystueberlapp.data.DateRange
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.OverallFigures
import de.landstueberl.mystueberlapp.data.PeriodFigures
import de.landstueberl.mystueberlapp.data.StatisticsDao
import de.landstueberl.mystueberlapp.data.YearStatistics
import de.landstueberl.mystueberlapp.data.yearRangeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Currency

/**
 * Turns the statistics DAO's raw cent values into [Money].
 *
 * The aggregates return plain integers — Room cannot apply the money converter
 * to a SUM column, since there is no currency in the result to reconstruct. The
 * shop trades only in euros, so the currency is supplied here.
 */
class StatisticsRepository(
    private val dao: StatisticsDao
) {

    fun getPeriodFiguresFlow(range: DateRange): Flow<PeriodFigures> =
        dao.getPeriodStatisticsFlow(range.from, range.to).map { stats ->
            PeriodFigures(
                revenueTotal = stats.revenueTotalCents.toMoney(),
                revenueSalesArea = stats.revenueSalesAreaCents.toMoney(),
                revenueOrders = stats.revenueOrdersCents.toMoney(),
                soldCount = stats.soldCount
            )
        }

    fun getOverallFiguresFlow(): Flow<OverallFigures> {
        val year = yearRangeOf(LocalDate.now().year)

        return combine(
            dao.getYearStatisticsFlow(year.from, year.to),
            dao.getAllTimeStatisticsFlow(),
            dao.getAverageStockPresenceFlow(),
            dao.getFreeProductCountFlow(year.from, year.to),
            dao.getRemovedProductCountFlow(year.from, year.to)
        ) { yearStats, allTime, stockPresence, freeCount, removedCount ->
            OverallFigures(
                profitThisYear = (yearStats.revenueCents - yearStats.costCents).toMoney(),
                averageSalePriceThisYear = yearStats.averageSalePrice(),
                averageSalePriceAllTime = allTime.averageSalePrice(),
                averageStockPresenceDays = stockPresence,
                highestSalePriceThisYear = yearStats.highestSaleCents.toMoney(),
                highestProfitThisYear = yearStats.highestProfitCents.toMoney(),
                freeProductsThisYear = freeCount,
                removedProductsThisYear = removedCount
            )
        }
    }

    // ── Conversion ─────────────────────────────
    private fun Long.toMoney(): Money = Money(
        amount = BigDecimal.valueOf(this, CENTS_SCALE),
        currency = CURRENCY
    )

    /**
     * Mean sale price, computed as SUM / COUNT rather than via SQL AVG so the
     * result stays an exact [BigDecimal] rather than a Double.
     */
    private fun YearStatistics.averageSalePrice(): Money? =
        averageOf(revenueCents, soldCount)

    private fun AllTimeStatistics.averageSalePrice(): Money? =
        averageOf(revenueCents, soldCount)

    private fun averageOf(totalCents: Long, count: Int): Money? {
        if (count == 0) return null
        return Money(
            amount = BigDecimal.valueOf(totalCents, CENTS_SCALE)
                .divide(BigDecimal(count), CENTS_SCALE, RoundingMode.HALF_UP),
            currency = CURRENCY
        )
    }

    private companion object {
        const val CENTS_SCALE = 2
        val CURRENCY: Currency = Currency.getInstance("EUR")
    }
}