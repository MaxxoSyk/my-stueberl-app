package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * NOTE: some queries embed [EventStatus] constant names as SQL string literals
 * ('CANCELLED'). Renaming an enum constant will not break compilation; it will
 * silently make those queries match nothing.
 */
@Dao
interface EventDao {

    // ── Writes ─────────────────────────────────
    @Upsert
    suspend fun upsertEvent(event: EventDetail): Long

    /**
     * Changes only the status, so a status switch can't overwrite fields edited
     * elsewhere. Which transitions are allowed is decided in the ViewModel.
     */
    @Query("UPDATE EventDetail SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: EventStatus)

    /** Product links are removed by CASCADE; the products themselves stay. */
    @Query("DELETE FROM EventDetail WHERE id = :id")
    suspend fun deleteEvent(id: Int)

    // ── Reads ──────────────────────────────────
    /**
     * All events, earliest first. Splitting into "upcoming" and "past" happens
     * in Kotlin, because it depends on today's date (see [EventDetail.lastDay]).
     */
    @Query("""
        SELECT e.*,
               (SELECT COUNT(*) FROM EventProductDetail ep WHERE ep.eventId = e.id) AS productCount
        FROM EventDetail e
        ORDER BY e.eventDate ASC, e.id ASC
    """)
    fun getEventsWithProductCountFlow(): Flow<List<EventWithProductCount>>

    /** Observed by the detail screen, so a status change shows up right away. */
    @Query("SELECT * FROM EventDetail WHERE id = :id")
    fun getEventFlow(id: Int): Flow<EventDetail?>

    /** One-time read for prefilling the edit form. */
    @Query("SELECT * FROM EventDetail WHERE id = :id")
    suspend fun getEventById(id: Int): EventDetail?

    /**
     * The nearest event that isn't over yet, skipping cancelled ones. A
     * multi-day event counts until its last day. Dates are epoch days, so
     * COALESCE and >= compare numerically.
     */
    @Query("""
        SELECT * FROM EventDetail
        WHERE status != 'CANCELLED'
          AND COALESCE(endDate, eventDate) >= :today
        ORDER BY eventDate ASC, id ASC
        LIMIT 1
    """)
    fun getNextEventFlow(today: LocalDate): Flow<EventDetail?>

    /** Every location entered so far, newest first. Grouped in Kotlin. */
    @Query("SELECT location FROM EventDetail WHERE location IS NOT NULL ORDER BY id DESC")
    suspend fun getAllLocationsNewestFirst(): List<String>
}