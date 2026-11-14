package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A product in the event's open basket. Stored in the database, so a
 * half-filled basket survives the app being closed. Keyed by product: a
 * product can only be in one basket.
 */
@Entity(
    foreignKeys = [
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
    indices = [Index("eventId")]
)
data class EventBasketItem(
    @PrimaryKey val productId: Int,
    val eventId: Int,
    /** Display order. The rowid can't be used: an Int primary key *is* the rowid. */
    val addedAt: Instant = Instant.now()
)