package de.landstueberl.mystueberlapp.repository

import de.landstueberl.mystueberlapp.data.db.dao.GalleryDao
import de.landstueberl.mystueberlapp.data.db.relation.GallerySection
import de.landstueberl.mystueberlapp.data.db.relation.groupByDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GalleryRepository(
    private val dao: GalleryDao
) {
    /** All product photos, grouped by the day they were added, newest first. */
    fun getGallerySectionsFlow(): Flow<List<GallerySection>> =
        dao.getGalleryEntriesFlow().map { it.groupByDate() }
}