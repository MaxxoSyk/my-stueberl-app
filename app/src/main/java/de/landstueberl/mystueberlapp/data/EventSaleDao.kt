package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import de.landstueberl.mystueberlapp.data.db.entity.EventBasketItem
import de.landstueberl.mystueberlapp.data.db.entity.EventSale
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Till: open basket and completed sales. The checks combining several of
 * these live in [de.landstueberl.mystueberlapp.repository.EventSaleRepository]
 * transactions.
 */
@Dao
interface EventSaleDao {

    // ── Guard Reads ────────────────────────────
    @Query("SELECT status FROM EventDetail WHERE id = :eventId")
    suspend fun getEventStatus(eventId: Int): EventStatus?

    @Query("SELECT status FROM ProductDetail WHERE id = :productId")
    suspend fun getProductStatus(productId: Int): ProductStatus?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM EventProductDetail WHERE eventId = :eventId AND productId = :productId
        )
    """)
    suspend fun isLinked(eventId: Int, productId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM EventBasketItem WHERE productId = :productId)")
    suspend fun isInBasket(productId: Int): Boolean

    // ── Basket ─────────────────────────────────
    @Insert
    suspend fun insertBasketItem(item: EventBasketItem)

    @Query("DELETE FROM EventBasketItem WHERE eventId = :eventId AND productId IN (:productIds)")
    suspend fun removeFromBasket(eventId: Int, productIds: List<Int>)

    @Query("DELETE FROM EventBasketItem WHERE eventId = :eventId")
    suspend fun clearBasket(eventId: Int)

    /** In the order the items were added. */
    @Transaction
    @Query("""
        SELECT p.* FROM ProductDetail p
        INNER JOIN EventBasketItem b ON b.productId = p.id
        WHERE b.eventId = :eventId
        ORDER BY b.addedAt ASC, b.productId ASC
    """)
    fun getBasketFlow(eventId: Int): Flow<List<Product>>

    @Transaction
    @Query("""
        SELECT p.* FROM ProductDetail p
        INNER JOIN EventBasketItem b ON b.productId = p.id
        WHERE b.eventId = :eventId
        ORDER BY b.addedAt ASC, b.productId ASC
    """)
    suspend fun getBasket(eventId: Int): List<Product>

    // ── Sales ──────────────────────────────────
    @Insert
    suspend fun insertSale(sale: EventSale): Long

    /** @return the number of products marked sold */
    @Query("""
        UPDATE ProductDetail
        SET status = 'SOLD', soldOn = :date, removedOn = NULL, eventSaleId = :saleId
        WHERE id IN (:productIds) AND status = 'AVAILABLE'
    """)
    suspend fun markSoldInSale(saleId: Int, productIds: List<Int>, date: LocalDate): Int

    @Query("SELECT * FROM EventSale WHERE id = :saleId")
    suspend fun getSale(saleId: Int): EventSale?

    /** Undo: back to available. The event links were never removed. */
    @Query("""
        UPDATE ProductDetail
        SET status = 'AVAILABLE', soldOn = NULL, eventSaleId = NULL
        WHERE eventSaleId = :saleId
    """)
    suspend fun releaseSaleProducts(saleId: Int)

    @Query("DELETE FROM EventSale WHERE id = :saleId")
    suspend fun deleteSale(saleId: Int)

    /** Newest first, for "recent sales" and the header figures. */
    @Query("SELECT * FROM EventSale WHERE eventId = :eventId ORDER BY soldAt DESC, id DESC")
    fun getSalesFlow(eventId: Int): Flow<List<EventSale>>

    /** Products sold at this event's till, for listing what each sale contained. */
    @Transaction
    @Query("""
        SELECT p.* FROM ProductDetail p
        INNER JOIN EventSale s ON s.id = p.eventSaleId
        WHERE s.eventId = :eventId
        ORDER BY p.id ASC
    """)
    fun getSoldProductsFlow(eventId: Int): Flow<List<Product>>
}