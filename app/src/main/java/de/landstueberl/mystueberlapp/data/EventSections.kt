package de.landstueberl.mystueberlapp.data

import java.time.LocalDate

/** The events list, split by date. Status plays no part in the split. */
data class EventSections(
    /** Not over yet (incl. running today), earliest start first. */
    val upcoming: List<EventWithProductCount>,
    /** Last day before today, most recent first. */
    val past: List<EventWithProductCount>
) {
    val isEmpty: Boolean
        get() = upcoming.isEmpty() && past.isEmpty()
}

/** Pure function, so it can be unit tested without Android. */
fun splitEvents(events: List<EventWithProductCount>, today: LocalDate): EventSections {
    val (upcoming, past) = events.partition { !it.event.lastDay.isBefore(today) }
    return EventSections(
        upcoming = upcoming.sortedWith(
            compareBy<EventWithProductCount> { it.event.eventDate }.thenBy { it.event.id }
        ),
        past = past.sortedWith(
            compareByDescending<EventWithProductCount> { it.event.lastDay }
                .thenByDescending { it.event.id }
        )
    )
}