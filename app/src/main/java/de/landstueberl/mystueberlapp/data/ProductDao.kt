package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COUNT(*) FROM ProductDetail WHERE isSold = 0 AND isRemoved = 0 AND orderId IS NULL")
    fun getAvailableProductsCountFlow(): Flow<Int>

    @Transaction
    @Query("""
    SELECT * FROM ProductDetail
    WHERE
        (
            (:includeAvailable = 1 AND isSold = 0 AND isRemoved = 0)
            OR (:includeSold = 1 AND isSold = 1)
            OR (:includeRemoved = 1 AND isRemoved = 1)
        )
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
        includeAvailable: Boolean,
        includeSold: Boolean,
        includeRemoved: Boolean,
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
}