package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** When the next event takes place, relative to today. */
sealed interface EventTiming {
    data object Today : EventTiming
    data object Tomorrow : EventTiming
    data class InDays(val days: Int) : EventTiming
    /** A multi-day event that has started and isn't on its last day yet. */
    data class RunsUntil(val lastDay: LocalDate) : EventTiming
}

data class NextEvent(
    val event: EventDetail,
    val timing: EventTiming
)

/**
 * Expects an event that isn't over yet (`lastDay >= today`), which
 * [EventDao.getNextEventFlow] guarantees. Pure, so it can be unit tested.
 */
fun eventTiming(event: EventDetail, today: LocalDate): EventTiming =
    if (!event.eventDate.isAfter(today)) {
        // Already started: today is its only or last day, or it keeps running.
        if (event.lastDay == today) EventTiming.Today else EventTiming.RunsUntil(event.lastDay)
    } else {
        val days = ChronoUnit.DAYS.between(today, event.eventDate).toInt()
        if (days == 1) EventTiming.Tomorrow else EventTiming.InDays(days)
    }