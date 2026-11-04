package de.landstueberl.mystueberlapp.data

import java.time.LocalDate

/** One photo in the gallery, with the product it belongs to. */
data class GalleryEntry(
    val fileName: String,
    val addedAt: LocalDate,
    /** A product sharing this photo — the lowest id, for batch photos. */
    val productId: Int
)

/** Photos added on one day. */
data class GallerySection(
    val date: LocalDate,
    val entries: List<GalleryEntry>
)

/**
 * Groups entries into date sections, preserving their order — so sections run
 * newest first, as does the query.
 */
fun List<GalleryEntry>.groupByDate(): List<GallerySection> =
    groupBy { it.addedAt }
        .map { (date, entries) -> GallerySection(date = date, entries = entries) }