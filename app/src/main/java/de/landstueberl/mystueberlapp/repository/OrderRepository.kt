package de.landstueberl.mystueberlapp.repository

import androidx.room.withTransaction
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderDao
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class OrderRepository(
    private val db: StueberlDatabase,
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {

    /**
     * Saves [order] together with its products and their images.
     *
     * Products that were previously part of the order but are absent from
     * [order] are **detached**, not deleted — they may already have been made,
     * and become sales-area stock instead.
     */
    suspend fun upsertOrderFull(order: Order): Order = db.withTransaction {
        val orderDetail = order.orderDetail

        // 1. Upsert the order itself
        val generatedOrderId = orderDao.upsertOrderDetail(orderDetail).toInt()
        val orderId = if (orderDetail.id == 0) generatedOrderId else orderDetail.id

        // 2. Which products does the order currently hold?
        val existingProductIds = orderDao.getProductsInOrder(orderId)
            .map { it.id }
            .toSet()

        // 3. Upsert incoming products and their images
        val incomingProductIds = mutableSetOf<Int>()

        for (product in order.productList) {
            val detail = product.details.copy(orderId = orderId)
            val generatedProductId = productDao.upsertProductDetail(detail).toInt()
            val productId = if (detail.id == 0) generatedProductId else detail.id

            incomingProductIds += productId

            // 3a. Sync this product's images
            val incomingImages = product.imageList.map { it.copy(productId = productId) }
            val incomingImageIds = incomingImages.map { it.id }.toSet()

            val imagesToDelete = productDao.getImagesByProductId(productId)
                .map { it.id }
                .filter { it !in incomingImageIds }

            if (imagesToDelete.isNotEmpty()) {
                productDao.deleteImagesByIds(imagesToDelete)
            }

            productDao.upsertImages(incomingImages)
        }

        // 4. Products dropped from the order become sales-area stock
        val productsToDetach = existingProductIds
            .minus(incomingProductIds)
            .toList()

        if (productsToDetach.isNotEmpty()) {
            productDao.detachProductsByIds(productsToDetach)
        }

        // 5. Return the order as persisted
        orderDao.getOrder(orderId)
            ?: error("Order $orderId vanished during save")
    }

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
}