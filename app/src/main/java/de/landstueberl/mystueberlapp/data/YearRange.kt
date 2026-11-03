package de.landstueberl.mystueberlapp.data

import java.time.LocalDate

/** First and last day of [year], for range queries over a date column. */
fun yearRangeOf(year: Int): DateRange = DateRange(
    from = LocalDate.of(year, 1, 1),
    to = LocalDate.of(year, 12, 31)
)