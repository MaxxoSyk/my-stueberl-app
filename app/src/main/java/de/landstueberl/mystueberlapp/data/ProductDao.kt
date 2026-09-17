package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * NOTE: several queries below embed [ProductStatus] constant names as SQL string
 * literals ('AVAILABLE', 'SOLD', 'REMOVED'). Renaming an enum constant will not
 * break compilation — it will silently make those queries match nothing.
 */
@Dao
interface ProductDao {

    // ── Writes ─────────────────────────────────
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

    @Query("DELETE FROM ProductDetail WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Int>)

    @Transaction
    suspend fun upsertProductWithImages(detail: ProductDetail, images: List<Image>): Product {
        // 1. Upsert ProductDetail
        val generatedId = upsertProductDetail(detail).toInt()
        val finalProductId = if (detail.id == 0) generatedId else detail.id

        // 2. Fetch existing images from DB
        val existingImages = getImagesByProductId(finalProductId)

        // 3. Ensure FK is correct on incoming images
        val updatedImages = images.map { img ->
            img.copy(productId = finalProductId)
        }

        // 4. Delete images that are no longer present
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

    // ── Reads ──────────────────────────────────
    @Transaction
    @Query("SELECT * FROM ProductDetail WHERE id = :id")
    suspend fun getProductById(id: Int): Product

    /**
     * Filtered product list. Ordering here is only for determinism — the display
     * order is applied in Kotlin via [ProductSortOrder].
     */
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
        ORDER BY createdAt ASC, id ASC
    """)
    fun getFilteredProductsFlow(
        statuses: List<String>,
        includeOrdered: Boolean,
        includeNotOrdered: Boolean,
        noYearFilter: Boolean,
        years: List<String>
    ): Flow<List<Product>>

    @Query("""
        SELECT DISTINCT strftime('%Y', createdAt) FROM ProductDetail
        ORDER BY createdAt DESC
    """)
    fun getAvailableYearsFlow(): Flow<List<String>>

    // ── Home Screen Counts ─────────────────────
    /** Sales-area stock that is still available. Excludes order products by design. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE status = 'AVAILABLE' AND orderId IS NULL
    """)
    fun getAvailableSalesAreaCountFlow(): Flow<Int>

    /** Products made for orders that have not been resolved yet. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE status = 'AVAILABLE' AND orderId IS NOT NULL
    """)
    fun getPendingOrderProductsCountFlow(): Flow<Int>

    /** Everything created in [year], both sales-area and order products. */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE strftime('%Y', createdAt) = :year
    """)
    fun getTotalProductsForYearFlow(year: String): Flow<Int>

    // ── Status Transitions (sales-area products) ──
    @Query("""
        UPDATE ProductDetail
        SET status = 'SOLD', soldOn = :date, removedOn = NULL
        WHERE id = :id
    """)
    suspend fun markAsSold(id: Int, date: LocalDate)

    @Query("""
        UPDATE ProductDetail
        SET status = 'REMOVED', removedOn = :date, soldOn = NULL
        WHERE id = :id
    """)
    suspend fun markAsRemoved(id: Int, date: LocalDate)

    @Query("""
        UPDATE ProductDetail
        SET status = 'AVAILABLE', soldOn = NULL, removedOn = NULL
        WHERE id = :id
    """)
    suspend fun resetToAvailable(id: Int)

    // ── Status Transitions (order-driven) ──────
    /** Order resolved: every product belonging to it counts as sold. */
    @Query("""
        UPDATE ProductDetail
        SET status = 'SOLD', soldOn = :date, removedOn = NULL
        WHERE orderId = :orderId AND status = 'AVAILABLE'
    """)
    suspend fun markOrderProductsAsSold(orderId: Int, date: LocalDate)

    /**
     * Order cancelled: products that were already made become sales-area stock.
     * Already-resolved products keep their order link as historical record.
     */
    @Query("""
        UPDATE ProductDetail
        SET orderId = NULL
        WHERE orderId = :orderId AND status = 'AVAILABLE'
    """)
    suspend fun detachProductsFromOrder(orderId: Int)
}