package de.landstueberl.mystueberlapp.data.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class DateConverters {

    /**
     * Dates are stored as **epoch days** — the integer count since 1970-01-01.
     *
     * Numeric comparison is unambiguous, so range queries read plainly as date
     * comparisons rather than relying on the lexicographic ordering of
     * ISO-8601 text.
     *
     * Note the trade-off: values are not human-readable in the database
     * inspector, and `strftime` no longer applies — year filters are expressed
     * as ranges computed in Kotlin instead.
     */
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
}