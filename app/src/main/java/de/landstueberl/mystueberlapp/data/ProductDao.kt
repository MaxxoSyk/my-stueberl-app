package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ProductDao {

    @Upsert
    suspend fun upsertProductDetail(detail: ProductDetail): Long

    @Upsert
    suspend fun upsertImage(image: Image): Long

    @Upsert
    suspend fun upsertImages(imageList: List<Image>)

    @Query("SELECT * FROM Image WHERE productId = :productId")
    suspend fun getImagesByProductId(productId: Int): List<Image>

    @Query("DELETE FROM Image WHERE id IN (:ids)")
    suspend fun deleteImagesByIds(ids: List<Int>)

    @Transaction
    @Query("SELECT * FROM ProductDetail WHERE id = :id")
    suspend fun getProductById(id: Int): Product

    @Transaction
    @Query("SELECT * FROM ProductDetail") // @todo all products mean also products from orders. Im not sure if its ok
    suspend fun getAllProducts(): List<Product>

    @Transaction
    @Query("SELECT * FROM ProductDetail") // @todo all products mean also products from orders. Im not sure if its ok
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("DELETE FROM ProductDetail WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Int>)

    @Query("SELECT COUNT(*) FROM ProductDetail WHERE status = 'AVAILABLE' AND orderId IS NULL")
    fun getAvailableProductsCountFlow(): Flow<Int>

    @Transaction
    @Query("""
    SELECT * FROM ProductDetail
    WHERE
        status IN (:statuses)
        AND (
            (:includeOrdered = 1 AND orderId IS NOT NULL)
            OR (:includeNotOrdered = 1 AND orderId IS NULL)
        )
        AND (
            :noYearFilter = 1
            OR strftime('%Y', createdAt) IN (:years)
        )
    ORDER BY
        removedOn ASC,
        createdAt ASC,
        description ASC
""")
    fun getFilteredProductsFlow(
        statuses: List<String>,
        includeOrdered: Boolean,
        includeNotOrdered: Boolean,
        noYearFilter: Boolean,
        years: List<String>
    ): Flow<List<Product>>

    @Query("SELECT DISTINCT strftime('%Y', createdAt) FROM ProductDetail ORDER BY createdAt DESC")
    fun getAvailableYearsFlow(): Flow<List<String>>

    @Query("""
    SELECT COUNT(*) FROM ProductDetail 
    WHERE strftime('%Y', createdAt) = :year
    AND orderId IS NULL
""")
    fun getTotalProductsForYearFlow(year: String): Flow<Int>

    @Query("""
    UPDATE ProductDetail 
    SET status = :status, soldOn = :date, removedOn = NULL 
    WHERE id = :id
""")
    suspend fun markAsSold(id: Int, status: ProductStatus, date: LocalDate)

    @Query("""
    UPDATE ProductDetail 
    SET status = :status, removedOn = :date, soldOn = NULL 
    WHERE id = :id
""")
    suspend fun markAsRemoved(id: Int, status: ProductStatus, date: LocalDate)

    @Query("""
    UPDATE ProductDetail 
    SET status = :status, soldOn = NULL, removedOn = NULL 
    WHERE id = :id
""")
    suspend fun resetToAvailable(id: Int, status: ProductStatus)

    @Transaction
    suspend fun upsertProductWithImages(detail: ProductDetail, images: List<Image>): Product {
        // 1. Upsert ProductDetail
        val generatedId = upsertProductDetail(detail).toInt()
        val finalProductId = if (detail.id == 0) generatedId else detail.id

        // 2. Fetch existing images from DB
        val existingImages = getImagesByProductId(finalProductId)

        // 3. Determine which images to keep & upsert
        val updatedImages = images.map { img ->
            img.copy(productId = finalProductId)
        }

        // 4. Determine which images to delete
        val incomingIds = updatedImages.map { it.id }.toSet()
        val toDelete = existingImages
            .filter { it.id !in incomingIds }
            .map { it.id }

        if (toDelete.isNotEmpty()) {
            deleteImagesByIds(toDelete)
        }

        // 5. Upsert new & updated images
        upsertImages(updatedImages)

        // 6. Return fresh product from DB
        return getProductById(finalProductId)
    }
}