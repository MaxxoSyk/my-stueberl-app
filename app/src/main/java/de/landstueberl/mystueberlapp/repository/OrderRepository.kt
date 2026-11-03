package de.landstueberl.mystueberlapp.repository

import androidx.room.withTransaction
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderDao
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.yearRangeOf
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class OrderRepository(
    private val db: StueberlDatabase,
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {

    // ── Status Transitions ─────────────────────
    /** Resolves the order and marks all of its products sold, atomically. */
    suspend fun resolveOrder(orderId: Int) = db.withTransaction {
        val today = LocalDate.now()
        orderDao.markOrderResolved(orderId, today)
        productDao.markOrderProductsAsSold(orderId = orderId, date = today)
    }

    /**
     * Cancels the order. Products already made are detached and become
     * sales-area stock, keeping their prices.
     */
    suspend fun cancelOrder(orderId: Int) = db.withTransaction {
        orderDao.markOrderCancelled(orderId, LocalDate.now())
        productDao.detachProductsFromOrder(orderId)
    }

    /**
     * Deletes the order outright. The foreign key is SET_NULL, so its products
     * survive as sales-area stock.
     */
    suspend fun deleteOrder(orderId: Int) {
        orderDao.deleteOrder(orderId)
    }

    fun getOrdersFlow(statuses: Set<OrderStatus>): Flow<List<Order>> =
        orderDao.getOrdersFlow(statuses.map { it.name })

    fun getOpenOrderCountFlow(): Flow<Int> =
        orderDao.getOpenOrderCountFlow()

    fun getNextDueOrderFlow(): Flow<OrderDetail?> =
        orderDao.getNextDueOrderFlow()

    fun getTotalOrdersForYearFlow(): Flow<Int> {
        val range = yearRangeOf(LocalDate.now().year)
        return orderDao.getTotalOrdersForRangeFlow(range.from, range.to)
    }

    suspend fun upsertOrderDetail(detail: OrderDetail): Int {
        val rowId = orderDao.upsertOrderDetail(detail).toInt()
        return if (detail.id == 0) rowId else detail.id
    }

    suspend fun getOrder(orderId: Int): Order? =
        orderDao.getOrder(orderId)

    fun getOrderFlow(orderId: Int): Flow<Order?> =
        orderDao.getOrderFlow(orderId)

    suspend fun detachProductsByIds(ids: List<Int>) =
        productDao.detachProductsByIds(ids)

    /** Sales-area products available to be attached to an order. */
    fun getAttachableProductsFlow(): Flow<List<Product>> =
        productDao.getAttachableProductsFlow()

    /**
     * Attaches products to [orderId]. The DAO re-checks that each is still
     * available and unattached, so a stale selection is skipped rather than
     * applied incorrectly.
     */
    suspend fun attachProductsToOrder(productIds: List<Int>, orderId: Int) =
        productDao.attachProductsToOrder(productIds, orderId)
}