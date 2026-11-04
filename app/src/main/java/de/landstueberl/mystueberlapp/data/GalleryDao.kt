package de.landstueberl.mystueberlapp.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Queries for the photo gallery.
 *
 * Kept separate from [ProductDao]: this reads the same tables but presents them
 * photo-first rather than product-first.
 */
@Dao
interface GalleryDao {

    /**
     * Every distinct product photo, newest first.
     *
     * Grouped by file name because batch members share one file — twenty rows
     * pointing at one photo should appear once. productId is the lowest id
     * among the sharers, giving a stable representative to navigate to.
     *
     * Only files referenced by an Image row are returned, so orphaned files on
     * disk never appear. The foreign key is CASCADE, so a row cannot outlive its
     * product either.
     */
    @Query("""
        SELECT
            fileName,
            MIN(addedAt) AS addedAt,
            MIN(productId) AS productId
        FROM Image
        GROUP BY fileName
        ORDER BY addedAt DESC, fileName ASC
    """)
    fun getGalleryEntriesFlow(): Flow<List<GalleryEntry>>
}