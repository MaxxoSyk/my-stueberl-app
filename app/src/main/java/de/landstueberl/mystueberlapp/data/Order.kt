package de.landstueberl.mystueberlapp.data

import androidx.room.Embedded
import androidx.room.Relation
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail

data class Order(
    @Embedded val orderDetail: OrderDetail,
    @Relation(
        entity = ProductDetail::class,
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val productList: List<OrderedProduct>
)