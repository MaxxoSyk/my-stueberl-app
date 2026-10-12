package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.landstueberl.mystueberlapp.data.OrderStatus
import java.time.LocalDate

@Entity(
    indices = [
        Index("status"),
        Index("dueDate")
    ]
)
data class OrderDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val dueDate: LocalDate,
    val note: String = "",
    val status: OrderStatus = OrderStatus.OPEN,
    val createdAt: LocalDate = LocalDate.now(),
    val resolvedOn: LocalDate? = null,
    val cancelledOn: LocalDate? = null
)
