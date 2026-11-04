package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
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
    suspend fun upsertProductFull(
        detail: ProductDetail,
        images: List<Image>,
        materials: List<ProductMaterialDetail>
    ): Product {
        // 1. Upsert ProductDetail
        val generatedId = upsertProductDetail(detail).toInt()
        val finalProductId = if (detail.id == 0) generatedId else detail.id

        // 2. Sync images
        val incomingImages = images.map { it.copy(productId = finalProductId) }
        val incomingImageIds = incomingImages.map { it.id }.toSet()

        val imagesToDelete = getImagesByProductId(finalProductId)
            .map { it.id }
            .filter { it !in incomingImageIds }

        if (imagesToDelete.isNotEmpty()) {
            deleteImagesByIds(imagesToDelete)
        }
        upsertImages(incomingImages)

        // 3. Sync materials — same diff-and-delete approach
        val incomingMaterials = materials.mapIndexed { index, material ->
            material.copy(productId = finalProductId, position = index)
        }
        val incomingMaterialIds = incomingMaterials.map { it.id }.toSet()

        val materialsToDelete = getMaterialsByProductId(finalProductId)
            .map { it.id }
            .filter { it !in incomingMaterialIds }

        if (materialsToDelete.isNotEmpty()) {
            deleteMaterialsByIds(materialsToDelete)
        }
        if (incomingMaterials.isNotEmpty()) {
            upsertMaterials(incomingMaterials)
        }

        // 4. Return fresh product from DB
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
        ORDER BY createdAt ASC, id ASC
    """)
    fun getFilteredProductsFlow(
        statuses: List<String>,
        includeOrdered: Boolean,
        includeNotOrdered: Boolean
    ): Flow<List<Product>>

    /** Creation dates of all products, for deriving which years have data. */
    @Query("SELECT DISTINCT createdAt FROM ProductDetail ORDER BY createdAt DESC")
    fun getProductCreationDatesFlow(): Flow<List<LocalDate>>

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

    /**
     * Everything created within [from]..[to], both sources.
     *
     * Dates are epoch days, so this is a numeric range comparison.
     */
    @Query("""
        SELECT COUNT(*) FROM ProductDetail
        WHERE createdAt >= :from AND createdAt <= :to
    """)
    fun getTotalProductsForRangeFlow(from: LocalDate, to: LocalDate): Flow<Int>

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

    /** Detaches specific products from whichever order they belong to. */
    @Query("UPDATE ProductDetail SET orderId = NULL WHERE id IN (:ids)")
    suspend fun detachProductsByIds(ids: List<Int>)

    /**
     * Sales-area products available to be attached to an order.
     *
     * Guarded to AVAILABLE and unattached: a sold or removed product cannot be
     * promised to a customer, and one already in an order must not be stolen
     * from it.
     */
    @Transaction
    @Query("""
        SELECT * FROM ProductDetail
        WHERE status = 'AVAILABLE' AND orderId IS NULL
        ORDER BY createdAt DESC, id ASC
    """)
    fun getAttachableProductsFlow(): Flow<List<Product>>

    /**
     * Attaches products to [orderId]. Re-checks the guard in SQL so a product
     * that changed state while the picker was open is silently skipped rather
     * than attached incorrectly.
     */
    @Query("""
        UPDATE ProductDetail
        SET orderId = :orderId
        WHERE id IN (:productIds)
          AND status = 'AVAILABLE'
          AND orderId IS NULL
    """)
    suspend fun attachProductsToOrder(productIds: List<Int>, orderId: Int)

    @Query("SELECT fileName FROM Image")
    suspend fun getAllImageFileNames(): List<String>

    /** The highest batch id in use, or null when no batches exist yet. */
    @Query("SELECT MAX(batchId) FROM ProductDetail")
    suspend fun getMaxBatchId(): Int?

    /**
     * Inserts [quantity] copies of [detail], all sharing a new batch id and —
     * if given — the same [imageFileName].
     *
     * The image file itself is shared, not copied: several Image rows may
     * reference one file, and the orphan sweep keeps a file alive while any
     * row still points at it.
     *
     * @return the batch id the products were created under
     */
    @Transaction
    suspend fun insertProductBatch(
        detail: ProductDetail,
        quantity: Int,
        imageFileName: String?,
        materials: List<ProductMaterialDetail>
    ): Int {
        require(quantity >= 1) { "quantity must be at least 1, was $quantity" }

        val batchId = (getMaxBatchId() ?: 0) + 1

        repeat(quantity) {
            val productId = upsertProductDetail(
                detail.copy(id = 0, batchId = batchId)
            ).toInt()

            if (imageFileName != null) {
                upsertImage(
                    Image(id = 0, productId = productId, fileName = imageFileName)
                )
            }

            // Each member gets its own copies, so editing one member's
            // calculation leaves the others untouched.
            if (materials.isNotEmpty()) {
                upsertMaterials(
                    materials.mapIndexed { index, material ->
                        material.copy(id = 0, productId = productId, position = index)
                    }
                )
            }
        }

        return batchId
    }

    // ── Materials ──────────────────────────────
    @Upsert
    suspend fun upsertMaterials(materials: List<ProductMaterialDetail>)

    @Query("SELECT * FROM ProductMaterialDetail WHERE productId = :productId ORDER BY position ASC")
    suspend fun getMaterialsByProductId(productId: Int): List<ProductMaterialDetail>

    @Query("DELETE FROM ProductMaterialDetail WHERE id IN (:ids)")
    suspend fun deleteMaterialsByIds(ids: List<Int>)

    @Query("DELETE FROM ProductMaterialDetail WHERE productId = :productId")
    suspend fun deleteMaterialsByProductId(productId: Int)

    /**
     * Every material name used so far, most frequently used first — so common
     * materials surface at the top of the calculator's suggestions.
     */
    @Query("""
        SELECT name FROM ProductMaterialDetail
        GROUP BY name COLLATE NOCASE
        ORDER BY COUNT(*) DESC, name COLLATE NOCASE ASC
    """)
    suspend fun getAllMaterialNames(): List<String>

    /**
     * The product and, when it belongs to a batch, its siblings — so the gallery
     * can show a shared photo's whole group.
     */
    @Transaction
    @Query("""
        SELECT * FROM ProductDetail
        WHERE id = :productId
           OR (
               batchId IS NOT NULL
               AND batchId = (SELECT batchId FROM ProductDetail WHERE id = :productId)
           )
        ORDER BY id ASC
    """)
    fun getProductWithBatchFlow(productId: Int): Flow<List<Product>>
}