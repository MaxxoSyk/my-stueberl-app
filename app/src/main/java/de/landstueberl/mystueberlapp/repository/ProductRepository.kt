package de.landstueberl.mystueberlapp.repository

import androidx.room.Transaction
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.collections.filter

class ProductRepository(private val dao: ProductDao) {

    @Transaction
    suspend fun upsertProduct(product: Product): Product {
        // 1. Upsert ProductDetail
        val detail = product.details
        val generatedId = dao.upsertProductDetail(detail).toInt()
        val finalProductId = if (detail.id == 0) generatedId else detail.id

        // 2. Fetch existing images from DB
        val existingImages = dao.getImagesByProductId(finalProductId)

        // 3. Determine which images to keep & upsert
        val updatedImages = product.imageList.map { img ->
            img.copy(
                productId = finalProductId,      // ensure FK is correct
                id = img.id                      // keep ID if exists (0 = insert new)
            )
        }

        // 4. Determine which images to delete
        val incomingIds = updatedImages.map { it.id }.toSet()
        val toDelete = existingImages
            .filter { it.id !in incomingIds }
            .map { it.id }

        if (toDelete.isNotEmpty()) {
            dao.deleteImagesByIds(toDelete)
        }

        // 5. Upsert new & updated images
        dao.upsertImages(updatedImages)

        // 6. Return fresh product from DB
        return dao.getProductById(finalProductId)
    }

    suspend fun deleteProductsByIds(ids: List<Int>) {
        dao.deleteProductsByIds(ids)
    }

    suspend fun markProductAsSold(productId: Int) {
        val product = dao.getProductById(productId)
        val updated = product.details.copy(
            isSold = true,
            isRemoved = false,
            removedOn = LocalDate.now()
        )
        dao.upsertProductDetail(updated)
    }

    suspend fun markProductAsRemoved(productId: Int) {
        val product = dao.getProductById(productId)
        val updated = product.details.copy(
            isRemoved = true,
            isSold = false,
            removedOn = LocalDate.now()
        )
        dao.upsertProductDetail(updated)
    }

    suspend fun resetProductToAvailable(productId: Int) {
        val product = dao.getProductById(productId)
        val updated = product.details.copy(
            isSold = false,
            isRemoved = false,
            removedOn = null
        )
        dao.upsertProductDetail(updated)
    }

    fun getAvailableProductsCountFlow(): Flow<Int> {
        return dao.getAvailableProductsCountFlow()
    }

    suspend fun getProductById(productId: Int): Product {
        return dao.getProductById(productId)
    }

    fun getFilteredProductsFlow(filter: ProductFilter): Flow<List<Product>> {
        val includeAvailable = filter.statuses.contains(ProductStatus.AVAILABLE)
        val includeSold = filter.statuses.contains(ProductStatus.SOLD)
        val includeRemoved = filter.statuses.contains(ProductStatus.REMOVED)
        val includeFromOrder = filter.productSourceFilter == ProductSourceFilter.ALL ||
                filter.productSourceFilter == ProductSourceFilter.FROM_ORDER
        val includeNotOrdered = filter.productSourceFilter == ProductSourceFilter.ALL ||
                filter.productSourceFilter == ProductSourceFilter.FROM_SALES_AREA
        val noYearFilter = filter.years.isEmpty()
        val years = filter.years.map { it.toString() }
            .ifEmpty { listOf("") }

        return dao.getFilteredProductsFlow(
            includeAvailable = includeAvailable,
            includeSold = includeSold,
            includeRemoved = includeRemoved,
            includeOrdered = includeFromOrder,
            includeNotOrdered = includeNotOrdered,
            noYearFilter = noYearFilter,
            years = years
        )
    }

    fun getAvailableYearsFlow(): Flow<List<String>> {
        return dao.getAvailableYearsFlow()
    }

    fun getTotalProductsForYearFlow(): Flow<Int> {
        return dao.getTotalProductsForYearFlow(
            year = LocalDate.now().year.toString()
        )
    }

}