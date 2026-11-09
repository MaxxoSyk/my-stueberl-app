package de.landstueberl.mystueberlapp.repository

import de.landstueberl.mystueberlapp.data.EventDao
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.EventWithProductCount
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class EventRepository(private val dao: EventDao) {

    /** @return the id of the saved event, new or existing */
    suspend fun saveEvent(event: EventDetail): Int {
        // @Upsert returns -1 when it updated an existing row.
        val generatedId = dao.upsertEvent(event).toInt()
        return if (event.id == 0) generatedId else event.id
    }

    suspend fun updateStatus(eventId: Int, status: EventStatus) =
        dao.updateStatus(id = eventId, status = status)

    suspend fun deleteEvent(eventId: Int) = dao.deleteEvent(eventId)

    fun getEventsWithProductCountFlow(): Flow<List<EventWithProductCount>> =
        dao.getEventsWithProductCountFlow()

    fun getEventFlow(eventId: Int): Flow<EventDetail?> = dao.getEventFlow(eventId)

    suspend fun getEventById(eventId: Int): EventDetail? = dao.getEventById(eventId)

    fun getNextEventFlow(today: LocalDate = LocalDate.now()): Flow<EventDetail?> =
        dao.getNextEventFlow(today)

    /**
     * Previously used locations, most used first, newest first on a tie.
     * Case is ignored (umlauts included, same as material names); the most
     * recent spelling wins.
     */
    suspend fun getLocationSuggestions(): List<String> =
        dao.getAllLocationsNewestFirst()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupBy { it.lowercase() }          // keeps first-seen (= newest) order
            .values
            .sortedByDescending { it.size }      // stable: ties stay newest first
            .map { it.first() }
}