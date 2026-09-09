package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ProductDetail::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class Image(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val filePath: String
)
