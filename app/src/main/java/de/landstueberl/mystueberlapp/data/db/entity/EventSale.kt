package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.landstueberl.mystueberlapp.data.model.common.Money
import java.time.Instant

/**
 * One customer's purchase at an event. Total and item count are a snapshot,
 * so later price edits don't change past event revenue.
 */
@Entity(
    foreignKeys = [
        // An event with sales can't be deleted: its revenue would vanish.
        ForeignKey(
            entity = EventDetail::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("eventId")]
)
data class EventSale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val soldAt: Instant,
    @Embedded(prefix = "total_") val total: Money,
    /** Sum of the sold products' purchase prices at the time of sale, for profit. */
    @Embedded(prefix = "purchase_total_") val purchaseTotal: Money,
    val itemCount: Int
)