package de.landstueberl.mystueberlapp.repository

import androidx.room.withTransaction
import de.landstueberl.mystueberlapp.data.EventDao
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.EventWithProductCount
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class EventRepository(
    private val db: StueberlDatabase,
    private val dao: EventDao
) {

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

    /** @see EventDao.cancelEvent */
    suspend fun cancelEvent(eventId: Int) = dao.cancelEvent(
        id = eventId,
        date = LocalDate.now(),
        cancelled = EventStatus.CANCELLED.name,
        cancellableStatuses = EventStatus.CANCELLABLE_NAMES
    )

    fun getEventProductsFlow(eventId: Int): Flow<List<Product>> =
        dao.getEventProductsFlow(eventId)

    fun getAttachableProductsFlow(eventId: Int): Flow<List<Product>> =
        dao.getAttachableProductsFlow(eventId, EventStatus.ACTIVE_NAMES)

    /** Products can only be assigned while the event is being prepared. */
    suspend fun linkProducts(eventId: Int, productIds: List<Int>) {
        if (productIds.isEmpty()) return
        dao.linkProducts(
            eventId = eventId,
            productIds = productIds,
            addedAt = LocalDate.now(),
            requiredStatus = EventStatus.IN_PREPARATION.name,
            activeStatuses = EventStatus.ACTIVE_NAMES
        )
    }

    suspend fun unlinkProducts(eventId: Int, productIds: List<Int>) {
        if (productIds.isEmpty()) return
        dao.unlinkProducts(eventId, productIds, EventStatus.IN_PREPARATION.name)
    }

    fun getAvailableProductCountFlow(eventId: Int): Flow<Int> =
        dao.getAvailableProductCountFlow(eventId)

    /** productId → eventId, for the "Event" badge and "Go to event". */
    fun getEventIdByProductFlow(): Flow<Map<Int, Int>> =
        dao.getActiveEventLinksFlow(EventStatus.ACTIVE_NAMES)
            .map { links -> links.associate { it.productId to it.eventId } }

    /**
     * Runs [createProducts] and links everything it created to the event, in
     * one transaction. Throws (and rolls back, so no products are created) if
     * the event is no longer being prepared, e.g. it was started or cancelled
     * while the form was open.
     */
    suspend fun createProductsForEvent(
        eventId: Int,
        createProducts: suspend () -> List<Int>
    ) {
        db.withTransaction {
            check(dao.getEventStatus(eventId) == EventStatus.IN_PREPARATION) {
                "Event $eventId is not in preparation"
            }
            linkProducts(eventId, createProducts())
        }
    }
}