package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ProductDetail::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId"),
        Index("addedAt")
    ]
)
data class Image(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val fileName: String,
    /**
     * When the photo was attached, which is not necessarily when the product
     * was created — a photo can be added or replaced later. The gallery groups
     * by this.
     */
    val addedAt: LocalDate = LocalDate.now()
)
