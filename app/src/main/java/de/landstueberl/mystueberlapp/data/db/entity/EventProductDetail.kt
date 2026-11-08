package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

/**
 * Links a product to an event. A product collects one link per event it was
 * taken to, which keeps the history after the event is finished.
 *
 * "At most one *active* event per product" depends on the event's status, which
 * lives in another table. SQLite can't enforce that, so the DAO checks it.
 */
@Entity(
    primaryKeys = ["eventId", "productId"],
    foreignKeys = [
        // Deleting an event removes its links, never its products.
        ForeignKey(
            entity = EventDetail::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductDetail::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // eventId is covered by the primary key index; productId needs its own.
    indices = [Index("productId")]
)
data class EventProductDetail(
    val eventId: Int,
    val productId: Int,
    val addedAt: LocalDate = LocalDate.now()
)