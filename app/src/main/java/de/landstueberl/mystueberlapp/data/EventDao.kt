package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.label.LabelPrint
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

    // ── Cancel ─────────────────────────────────
    /**
     * Cancels the event if it hasn't started yet. Links stay as history; the
     * products are free again because the event is no longer active.
     */
    @Query("""
        UPDATE EventDetail
        SET status = :cancelled, cancelledOn = :date
        WHERE id = :id AND status IN (:cancellableStatuses)
    """)
    suspend fun cancelEvent(
        id: Int,
        date: LocalDate,
        cancelled: String,
        cancellableStatuses: List<String>
    )

    // ── Event Products ─────────────────────────
    /**
     * Every product linked to the event, in the order it was assigned.
     * `ep.rowid` keeps insertion order within a day, since `addedAt` has no time.
     * Selecting only `p.*` avoids column clashes with the join table.
     */
    @Transaction
    @Query("""
        SELECT p.* FROM ProductDetail p
        INNER JOIN EventProductDetail ep ON ep.productId = p.id
        WHERE ep.eventId = :eventId
        ORDER BY ep.addedAt ASC, ep.rowid ASC
    """)
    fun getEventProductsFlow(eventId: Int): Flow<List<Product>>

    /**
     * Stand stock that could be taken to the event: available, not in an
     * order, not in any active event, and not linked to this event already.
     */
    @Transaction
    @Query("""
        SELECT * FROM ProductDetail p
        WHERE p.status = 'AVAILABLE'
          AND p.orderId IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM EventProductDetail ep
              INNER JOIN EventDetail e ON e.id = ep.eventId
              WHERE ep.productId = p.id AND e.status IN (:activeStatuses)
          )
          AND NOT EXISTS (
              SELECT 1 FROM EventProductDetail ep
              WHERE ep.productId = p.id AND ep.eventId = :eventId
          )
        ORDER BY p.createdAt DESC, p.id ASC
    """)
    fun getAttachableProductsFlow(
        eventId: Int,
        activeStatuses: List<String>
    ): Flow<List<Product>>

    /**
     * Links products to the event in one guarded statement. A product is skipped
     * if it was sold, put in an order or taken to another event meanwhile, and
     * nothing is linked unless the event is still in [requiredStatus].
     */
    @Query("""
        INSERT OR IGNORE INTO EventProductDetail (eventId, productId, addedAt)
        SELECT :eventId, p.id, :addedAt FROM ProductDetail p
        WHERE p.id IN (:productIds)
          AND p.status = 'AVAILABLE'
          AND p.orderId IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM EventProductDetail ep
              INNER JOIN EventDetail e ON e.id = ep.eventId
              WHERE ep.productId = p.id AND e.status IN (:activeStatuses)
          )
          AND EXISTS (
              SELECT 1 FROM EventDetail WHERE id = :eventId AND status = :requiredStatus
          )
    """)
    suspend fun linkProducts(
        eventId: Int,
        productIds: List<Int>,
        addedAt: LocalDate,
        requiredStatus: String,
        activeStatuses: List<String>
    )

    /** Removes links, but only while the event is still in [requiredStatus]. */
    @Query("""
        DELETE FROM EventProductDetail
        WHERE eventId = :eventId
          AND productId IN (:productIds)
          AND EXISTS (
              SELECT 1 FROM EventDetail WHERE id = :eventId AND status = :requiredStatus
          )
    """)
    suspend fun unlinkProducts(eventId: Int, productIds: List<Int>, requiredStatus: String)

    /** For the home tile: what is actually being taken along. */
    @Query("""
        SELECT COUNT(*) FROM EventProductDetail ep
        INNER JOIN ProductDetail p ON p.id = ep.productId
        WHERE ep.eventId = :eventId AND p.status = 'AVAILABLE'
    """)
    fun getAvailableProductCountFlow(eventId: Int): Flow<Int>

    /**
     * Available products held by an active event, with that event. A product is
     * in at most one active event, so each productId appears once. Sold or
     * removed products are left out: they were sold at the stand (rule 1a).
     */
    @Query("""
        SELECT ep.productId AS productId, ep.eventId AS eventId
        FROM EventProductDetail ep
        INNER JOIN EventDetail e ON e.id = ep.eventId
        INNER JOIN ProductDetail p ON p.id = ep.productId
        WHERE e.status IN (:activeStatuses) AND p.status = 'AVAILABLE'
    """)
    fun getActiveEventLinksFlow(activeStatuses: List<String>): Flow<List<ProductEventLink>>

    /** Single-column read for guards inside transactions. Null if the event is gone. */
    @Query("SELECT status FROM EventDetail WHERE id = :id")
    suspend fun getEventStatus(id: Int): EventStatus?

    /** Products of the event whose label was confirmed as printed. */
    @Query("""
        SELECT productId, labelPrintedAt FROM EventProductDetail
        WHERE eventId = :eventId AND labelPrintedAt IS NOT NULL
    """)
    fun getLabelPrintsFlow(eventId: Int): Flow<List<LabelPrint>>

    /** Only while labels can be printed for the event (guarded like linking). */
    @Query("""
        UPDATE EventProductDetail
        SET labelPrintedAt = :date
        WHERE eventId = :eventId
          AND productId IN (:productIds)
          AND EXISTS (
              SELECT 1 FROM EventDetail WHERE id = :eventId AND status IN (:printableStatuses)
          )
    """)
    suspend fun markLabelsPrinted(
        eventId: Int,
        productIds: List<Int>,
        date: LocalDate,
        printableStatuses: List<String>
    )
}