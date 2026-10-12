package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * NOTE: several queries below embed [OrderStatus] and [ProductStatus] constant
 * names as SQL string literals. Renaming a constant will not break compilation —
 * it will silently make those queries match nothing.
 */
@Dao
interface OrderDao {

    // ── Writes ─────────────────────────────────
    @Upsert
    suspend fun upsertOrderDetail(orderDetail: OrderDetail): Long

    @Query("DELETE FROM OrderDetail WHERE id = :orderId")
    suspend fun deleteOrder(orderId: Int)

    // ── Reads ──────────────────────────────────
    @Transaction
    @Query("SELECT * FROM OrderDetail WHERE id = :orderId")
    suspend fun getOrder(orderId: Int): Order?

    @Transaction
    @Query("SELECT * FROM OrderDetail WHERE id = :orderId")
    fun getOrderFlow(orderId: Int): Flow<Order?>

    @Transaction
    @Query("""
        SELECT * FROM OrderDetail
        WHERE status IN (:statuses)
        ORDER BY dueDate ASC, id ASC
    """)
    fun getOrdersFlow(statuses: List<String>): Flow<List<Order>>

    @Query("SELECT * FROM ProductDetail WHERE orderId = :orderId")
    suspend fun getProductsInOrder(orderId: Int): List<ProductDetail>

    // ── Home Screen ────────────────────────────
    @Query("SELECT COUNT(*) FROM OrderDetail WHERE status = 'OPEN'")
    fun getOpenOrderCountFlow(): Flow<Int>

    /** The open order with the nearest due date, or null if there are none. */
    @Query("""
        SELECT * FROM OrderDetail
        WHERE status = 'OPEN'
        ORDER BY dueDate ASC, id ASC
        LIMIT 1
    """)
    fun getNextDueOrderFlow(): Flow<OrderDetail?>

    @Query("""
        SELECT COUNT(*) FROM OrderDetail
        WHERE strftime('%Y', createdAt) = :year
    """)
    fun getTotalOrdersForYearFlow(year: String): Flow<Int>

    // ── Status Transitions ─────────────────────
    @Query("""
        UPDATE OrderDetail
        SET status = 'RESOLVED', resolvedOn = :date, cancelledOn = NULL
        WHERE id = :orderId AND status = 'OPEN'
    """)
    suspend fun markOrderResolved(orderId: Int, date: LocalDate)

    @Query("""
        UPDATE OrderDetail
        SET status = 'CANCELLED', cancelledOn = :date, resolvedOn = NULL
        WHERE id = :orderId AND status = 'OPEN'
    """)
    suspend fun markOrderCancelled(orderId: Int, date: LocalDate)

    // ── Product Attachment ─────────────────────
    /**
     * Attaches an existing product to an order. Guarded to AVAILABLE so an
     * already sold or removed product cannot be pulled into a new order.
     */
    @Query("""
        UPDATE ProductDetail
        SET orderId = :orderId
        WHERE id = :productId AND status = 'AVAILABLE' AND orderId IS NULL
    """)
    suspend fun attachProductToOrder(productId: Int, orderId: Int)

    /** Sales-area products eligible to be attached to an order. */
    @Transaction
    @Query("""
        SELECT * FROM ProductDetail
        WHERE status = 'AVAILABLE' AND orderId IS NULL
        ORDER BY createdAt DESC, id ASC
    """)
    fun getAttachableProductsFlow(): Flow<List<Product>>
}