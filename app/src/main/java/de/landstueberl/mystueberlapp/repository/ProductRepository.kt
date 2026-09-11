package de.landstueberl.mystueberlapp.repository

import androidx.room.Transaction
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductDao
import kotlinx.coroutines.flow.Flow
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
        return dao.getProduct(finalProductId)
    }

    suspend fun getAllProducts(): List<Product> {
        return dao.getAllProducts()
    }

    fun getAllProductsFlow(): Flow<List<Product>> {
        return dao.getAllProductsFlow()
    }

    suspend fun deleteProductsByIds(ids: List<Int>) {
        dao.deleteProductsByIds(ids)
    }

}