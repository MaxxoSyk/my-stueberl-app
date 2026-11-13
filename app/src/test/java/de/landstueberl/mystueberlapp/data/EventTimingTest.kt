package de.landstueberl.mystueberlapp.data

import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventTimingTest {

    private val today = LocalDate.of(2025, 10, 12)

    private fun event(start: LocalDate, end: LocalDate? = null) =
        EventDetail(name = "Test", eventDate = start, endDate = end, createdAt = today)

    @Test
    fun `one-day event today`() =
        assertEquals(EventTiming.Today, eventTiming(event(today), today))

    @Test
    fun `last day of a multi-day event is today`() =
        assertEquals(EventTiming.Today, eventTiming(event(today.minusDays(2), today), today))

    @Test
    fun `multi-day event in progress runs until its last day`() =
        assertEquals(
            EventTiming.RunsUntil(today.plusDays(1)),
            eventTiming(event(today.minusDays(1), today.plusDays(1)), today)
        )

    @Test
    fun `tomorrow and later`() {
        assertEquals(EventTiming.Tomorrow, eventTiming(event(today.plusDays(1)), today))
        assertEquals(EventTiming.InDays(5), eventTiming(event(today.plusDays(5)), today))
    }

    @Test
    fun `past last day is still running`() {
        assertEquals(EventTiming.StillRunning, eventTiming(event(today.minusDays(1)), today))
        assertEquals(
            EventTiming.StillRunning,
            eventTiming(event(today.minusDays(3), today.minusDays(1)), today)
        )
    }
}