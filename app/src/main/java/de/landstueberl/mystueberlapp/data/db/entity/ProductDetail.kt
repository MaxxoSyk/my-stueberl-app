package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.ProductStatus
import java.time.LocalDate

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = OrderDetail::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("orderId"),
        Index("status")
    ]
)
data class ProductDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    @Embedded(prefix = "purchase_") val purchasePrice: Money?,
    @Embedded(prefix = "sales_") val salesPrice: Money?,
    val status: ProductStatus = ProductStatus.AVAILABLE,
    val orderId: Int? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val soldOn: LocalDate? = null,
    val removedOn: LocalDate? = null
)