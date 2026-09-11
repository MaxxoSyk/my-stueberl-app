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
    suspend fun getProduct(id: Int): Product

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
}