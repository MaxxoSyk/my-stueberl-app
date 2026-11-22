package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

/**
 * A printed group label: one QR code for a whole batch at one event. Covers
 * every member of the batch at that event, including ones added later.
 * No foreign key on batchId: batches have no table of their own.
 */
@Entity(
    primaryKeys = ["eventId", "batchId"],
    foreignKeys = [
        ForeignKey(
            entity = EventDetail::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId", "labelIdentifier"], unique = true)]
)
data class EventBatchLabel(
    val eventId: Int,
    val batchId: Int,
    val printedAt: LocalDate,
    /** Shared by all members while the group label applies; kept on reprint. */
    val labelIdentifier: String
)