package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.landstueberl.mystueberlapp.data.Money

/**
 * One line of a product's cost calculation.
 *
 * A product with no rows has a manually entered purchase price; a product with
 * rows has a calculated one, and its price field is read-only.
 */
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
data class ProductMaterialDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val name: String,
    val quantity: Int = 1,
    @Embedded(prefix = "unit_") val unitPrice: Money,
    val position: Int = 0
)