package de.landstueberl.mystueberlapp.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The period the selectable statistics rows apply to. */
enum class StatisticsPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR
}

/** A closed date range, both ends inclusive. */
data class DateRange(
    val from: LocalDate,
    val to: LocalDate
)

/**
 * The calendar range this period covers, relative to [today].
 *
 * Weeks run Monday–Sunday. Ranges span the **whole** period rather than
 * stopping at today: future-dated sales are already prevented by the date
 * pickers, so there is nothing to exclude and the full range reads more
 * honestly as "this month".
 */
fun StatisticsPeriod.rangeOf(today: LocalDate = LocalDate.now()): DateRange = when (this) {
    StatisticsPeriod.TODAY -> DateRange(today, today)

    StatisticsPeriod.WEEK -> DateRange(
        from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        to = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    )

    StatisticsPeriod.MONTH -> DateRange(
        from = today.withDayOfMonth(1),
        to = today.withDayOfMonth(today.lengthOfMonth())
    )

    StatisticsPeriod.YEAR -> DateRange(
        from = today.withDayOfYear(1),
        to = today.withDayOfYear(today.lengthOfYear())
    )
}