package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail

@Dao
interface OrderDao {

    @Upsert
    suspend fun upsertOrderDetail(orderDetail: OrderDetail): Long

    @Transaction
    @Query("SELECT * FROM OrderDetail WHERE id = :orderId")
    suspend fun getOrder(orderId: Int): Order

    @Query("SELECT * FROM ProductDetail WHERE orderId = :orderId")
    suspend fun getProductsInOrder(orderId: Int): List<ProductDetail>

    @Query("DELETE FROM ProductDetail WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Int>)
}