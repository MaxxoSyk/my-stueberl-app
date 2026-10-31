package de.landstueberl.mystueberlapp.util

import java.time.LocalDate

/**
 * Material3's DatePicker exposes its selection as milliseconds **interpreted as
 * UTC midnight**. Converting a [LocalDate] through a timezone-aware instant
 * therefore lands a day early in any zone ahead of UTC — in Berlin, a date set to
 * the 31st pre-selects the 30th.
 *
 * A calendar date is a day count, not an instant, so these conversions treat it
 * as one and avoid timezones entirely.
 */
private const val MILLIS_PER_DAY = 86_400_000L

/** This date as UTC-midnight millis, for `initialSelectedDateMillis`. */
fun LocalDate.toPickerMillis(): Long = toEpochDay() * MILLIS_PER_DAY

/** A DatePicker selection back to the calendar date it represents. */
fun Long.toLocalDateFromPicker(): LocalDate =
    LocalDate.ofEpochDay(Math.floorDiv(this, MILLIS_PER_DAY))