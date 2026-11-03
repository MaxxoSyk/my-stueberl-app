package de.landstueberl.mystueberlapp.view.statistics

import androidx.annotation.StringRes
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.StatisticsPeriod

@get:StringRes
val StatisticsPeriod.labelRes: Int
    get() = when (this) {
        StatisticsPeriod.TODAY -> R.string.statistics_period_today
        StatisticsPeriod.WEEK -> R.string.statistics_period_week
        StatisticsPeriod.MONTH -> R.string.statistics_period_month
        StatisticsPeriod.YEAR -> R.string.statistics_period_year
    }