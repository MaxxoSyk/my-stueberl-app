package de.landstueberl.mystueberlapp.repository

import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ProductRepository(private val dao: ProductDao) {

    suspend fun upsertProduct(product: Product): Product {
        return dao.upsertProductWithImages(product.details, product.imageList)
    }

    suspend fun deleteProductsByIds(ids: List<Int>) {
        dao.deleteProductsByIds(ids)
    }

    // ── Sales-area products only ───────────────
    suspend fun markProductAsSold(productId: Int, soldOn: LocalDate) {
        dao.markAsSold(id = productId, date = soldOn)
    }

    suspend fun markProductAsRemoved(productId: Int, removedOn: LocalDate) {
        dao.markAsRemoved(id = productId, date = removedOn)
    }

    suspend fun resetProductToAvailable(productId: Int) {
        dao.resetToAvailable(id = productId)
    }

    fun getAvailableSalesAreaCountFlow(): Flow<Int> {
        return dao.getAvailableSalesAreaCountFlow()
    }

    fun getPendingOrderProductsCountFlow(): Flow<Int> =
        dao.getPendingOrderProductsCountFlow()

    fun getTotalProductsForYearFlow(): Flow<Int> =
        dao.getTotalProductsForYearFlow(year = LocalDate.now().year.toString())

    fun getAvailableYearsFlow(): Flow<List<String>> {
        return dao.getAvailableYearsFlow()
    }

    suspend fun getProductById(productId: Int): Product {
        return dao.getProductById(productId)
    }

    fun getFilteredProductsFlow(filter: ProductFilter): Flow<List<Product>> {
        val statuses = filter.statuses.map { it.name }
        val includeFromOrder = filter.productSourceFilter == ProductSourceFilter.ALL ||
                filter.productSourceFilter == ProductSourceFilter.FROM_ORDER
        val includeNotOrdered = filter.productSourceFilter == ProductSourceFilter.ALL ||
                filter.productSourceFilter == ProductSourceFilter.FROM_SALES_AREA
        val noYearFilter = filter.years.isEmpty()
        val years = filter.years.map { it.toString() }
            .ifEmpty { listOf("") }

        return dao.getFilteredProductsFlow(
            statuses = statuses,
            includeOrdered = includeFromOrder,
            includeNotOrdered = includeNotOrdered,
            noYearFilter = noYearFilter,
            years = years
        )
    }

    // ── Order-driven transitions ───────────────
    suspend fun markOrderProductsAsSold(orderId: Int) {
        dao.markOrderProductsAsSold(orderId = orderId, date = LocalDate.now())
    }

    suspend fun detachProductsFromOrder(orderId: Int) {
        dao.detachProductsFromOrder(orderId)
    }

    suspend fun getAllImageFileNames(): List<String> = dao.getAllImageFileNames()

    /**
     * Creates [quantity] identical products as one batch.
     * @return the generated batch id
     */
    suspend fun insertProductBatch(
        detail: ProductDetail,
        quantity: Int,
        imageFileName: String?
    ): Int = dao.insertProductBatch(
        detail = detail,
        quantity = quantity,
        imageFileName = imageFileName
    )
}