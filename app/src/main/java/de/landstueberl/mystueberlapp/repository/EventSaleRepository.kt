package de.landstueberl.mystueberlapp.repository

import androidx.room.withTransaction
import de.landstueberl.mystueberlapp.data.BasketAddResult
import de.landstueberl.mystueberlapp.data.EventSaleDao
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.data.SaleResult
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.db.entity.EventBasketItem
import de.landstueberl.mystueberlapp.data.db.entity.EventSale
import de.landstueberl.mystueberlapp.data.sumMoney
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class EventSaleRepository(
    private val db: StueberlDatabase,
    private val dao: EventSaleDao
) {

    fun getBasketFlow(eventId: Int): Flow<List<Product>> = dao.getBasketFlow(eventId)

    fun getSalesFlow(eventId: Int): Flow<List<EventSale>> = dao.getSalesFlow(eventId)

    /** Checks run in one transaction, so nothing can change between check and insert. */
    suspend fun addToBasket(eventId: Int, productId: Int): BasketAddResult = db.withTransaction {
        when {
            dao.getEventStatus(eventId) != EventStatus.IN_PROGRESS -> BasketAddResult.EventNotRunning
            !dao.isLinked(eventId, productId) -> BasketAddResult.NotInEvent
            dao.getProductStatus(productId) != ProductStatus.AVAILABLE -> BasketAddResult.NotAvailable
            dao.isInBasket(productId) -> BasketAddResult.AlreadyInBasket
            else -> {
                dao.insertBasketItem(EventBasketItem(productId = productId, eventId = eventId))
                BasketAddResult.Added
            }
        }
    }

    suspend fun removeFromBasket(eventId: Int, productIds: List<Int>) {
        if (productIds.isEmpty()) return
        dao.removeFromBasket(eventId, productIds)
    }

    suspend fun clearBasket(eventId: Int) = dao.clearBasket(eventId)

    /**
     * Sells the basket in one transaction. [expectedProductIds] is what the
     * screen showed the customer; if the basket differs, nothing is sold.
     */
    suspend fun completeSale(eventId: Int, expectedProductIds: Set<Int>): SaleResult =
        db.withTransaction {
            if (dao.getEventStatus(eventId) != EventStatus.IN_PROGRESS) {
                return@withTransaction SaleResult.EventNotRunning
            }

            val (valid, outdated) = dao.getBasket(eventId)
                .partition { it.details.status == ProductStatus.AVAILABLE }
            // Removing outdated items is kept (the transaction commits normally).
            removeFromBasket(eventId, outdated.map { it.details.id })

            val ids = valid.map { it.details.id }
            if (outdated.isNotEmpty() || ids.toSet() != expectedProductIds) {
                return@withTransaction SaleResult.BasketChanged
            }
            if (ids.isEmpty()) return@withTransaction SaleResult.EmptyBasket

            val sale = EventSale(
                eventId = eventId,
                soldAt = Instant.now(),
                total = valid.sumMoney { it.details.salesPrice },
                purchaseTotal = valid.sumMoney { it.details.purchasePrice },
                itemCount = ids.size
            )
            val saleId = dao.insertSale(sale).toInt()

            val sold = dao.markSoldInSale(saleId, ids, LocalDate.now())
            check(sold == ids.size) { "Expected $ids.size products sold, was $sold" }

            dao.clearBasket(eventId)
            SaleResult.Completed(sale.copy(id = saleId))
        }

    /** @return false if the sale doesn't exist or its event isn't running anymore */
    suspend fun undoSale(saleId: Int): Boolean = db.withTransaction {
        val sale = dao.getSale(saleId) ?: return@withTransaction false
        if (dao.getEventStatus(sale.eventId) != EventStatus.IN_PROGRESS) {
            return@withTransaction false
        }
        dao.releaseSaleProducts(saleId)
        dao.deleteSale(saleId)
        true
    }

    fun getSoldProductsFlow(eventId: Int): Flow<List<Product>> = dao.getSoldProductsFlow(eventId)

    fun getBasketCountFlow(eventId: Int): Flow<Int> = dao.getBasketCountFlow(eventId)

    fun hasSalesFlow(eventId: Int): Flow<Boolean> = dao.hasSalesFlow(eventId)
}