package de.landstueberl.mystueberlapp.repository

import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.MaterialSuggestion
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.buildMaterialSuggestions
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import de.landstueberl.mystueberlapp.data.yearRangeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ProductRepository(private val dao: ProductDao) {

    suspend fun upsertProduct(product: Product): Product =
        dao.upsertProductFull(
            detail = product.details,
            images = product.imageList,
            materials = product.materialList
        )

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

    fun getAvailableSalesAreaCountFlow(): Flow<Int> =
        dao.getAvailableSalesAreaCountFlow(EventStatus.ACTIVE_NAMES)

    fun getPendingOrderProductsCountFlow(): Flow<Int> =
        dao.getPendingOrderProductsCountFlow()

    fun getTotalProductsForYearFlow(): Flow<Int> {
        val range = yearRangeOf(LocalDate.now().year)
        return dao.getTotalProductsForRangeFlow(range.from, range.to)
    }

    /** Years in which products were created, newest first. */
    fun getAvailableYearsFlow(): Flow<List<Int>> =
        dao.getProductCreationDatesFlow()
            .map { dates -> dates.map { it.year }.distinct() }

    suspend fun getProductById(productId: Int): Product {
        return dao.getProductById(productId)
    }

    fun getFilteredProductsFlow(filter: ProductFilter): Flow<List<Product>> {
        // Exhaustive on purpose: a new source must be handled here, or it
        // won't compile, instead of being silently ignored by the query.
        val source = when (filter.productSourceFilter) {
            ProductSourceFilter.ALL -> SourceFlags(ordered = true, event = true, salesArea = true)
            ProductSourceFilter.FROM_ORDER -> SourceFlags(ordered = true)
            ProductSourceFilter.FROM_EVENT -> SourceFlags(event = true)
            ProductSourceFilter.FROM_SALES_AREA -> SourceFlags(salesArea = true)
        }

        return dao.getFilteredProductsFlow(
            statuses = filter.statuses.map { it.name },
            includeOrdered = source.ordered,
            includeEvent = source.event,
            includeSalesArea = source.salesArea,
            activeEventStatuses = EventStatus.ACTIVE_NAMES
        ).map { products ->
            // The year filter moved out of SQL: with epoch days there is no
            // strftime equivalent, and an arbitrary set of years doesn't reduce
            // to a single range.
            if (filter.years.isEmpty()) {
                products
            } else {
                products.filter { it.details.createdAt.year in filter.years }
            }
        }
    }

    private data class SourceFlags(
        val ordered: Boolean = false,
        val event: Boolean = false,
        val salesArea: Boolean = false
    )

    suspend fun getAllImageFileNames(): List<String> = dao.getAllImageFileNames()

    /**
     * Creates [quantity] identical products as one batch.
     * @return the generated batch id
     */
    suspend fun insertProductBatch(
        detail: ProductDetail,
        quantity: Int,
        imageFileName: String?,
        materials: List<ProductMaterialDetail>
    ): Int = dao.insertProductBatch(
        detail = detail,
        quantity = quantity,
        imageFileName = imageFileName,
        materials = materials
    )

    suspend fun getProductIdsByBatchId(batchId: Int): List<Int> =
        dao.getProductIdsByBatchId(batchId)

    suspend fun getMaterialSuggestions(): List<MaterialSuggestion> =
        buildMaterialSuggestions(dao.getMaterialPriceUsages())

    fun getProductWithBatchFlow(productId: Int): Flow<List<Product>> =
        dao.getProductWithBatchFlow(productId)

    fun searchProductsFlow(query: String): Flow<List<Product>> =
        dao.searchProductsFlow(query)
}