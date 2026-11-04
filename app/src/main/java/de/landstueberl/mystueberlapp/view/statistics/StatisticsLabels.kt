package de.landstueberl.mystueberlapp.view.statistics

import androidx.annotation.StringRes
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.DateRange
import de.landstueberl.mystueberlapp.data.StatisticsPeriod
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@get:StringRes
val StatisticsPeriod.labelRes: Int
    get() = when (this) {
        StatisticsPeriod.TODAY -> R.string.statistics_period_today
        StatisticsPeriod.WEEK -> R.string.statistics_period_week
        StatisticsPeriod.MONTH -> R.string.statistics_period_month
        StatisticsPeriod.YEAR -> R.string.statistics_period_year
    }

private val RANGE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private val RANGE_DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.")

/**
 * The range as a caption, e.g. "02.11. – 08.11.2026". A single day shows once;
 * longer ranges abbreviate the start so the line stays short.
 */
fun DateRange.asCaption(): String =
    if (from == to) {
        from.format(RANGE_FORMAT)
    } else {
        "${from.format(RANGE_DAY_MONTH)} – ${to.format(RANGE_FORMAT)}"
    }