package de.landstueberl.mystueberlapp.data

import androidx.annotation.StringRes
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import java.time.LocalDate

@get:StringRes
val ProductSortOrder.labelRes: Int
    get() = when (this) {
        ProductSortOrder.OLDEST_FIRST -> R.string.products_sort_oldest
        ProductSortOrder.NEWEST_FIRST -> R.string.products_sort_newest
        ProductSortOrder.RECENT_ACTIVITY -> R.string.products_sort_activity
    }

enum class ProductSortOrder {
    /** Available first, oldest at the top — stale stock is the removal candidate. */
    OLDEST_FIRST,

    /** Available first, newest additions at the top. */
    NEWEST_FIRST,

    /** One timeline across all statuses, most recent activity first. */
    RECENT_ACTIVITY
}

/** Sales-area products first, then sold, then removed. */
private val ProductStatus.rank: Int
    get() = when (this) {
        ProductStatus.AVAILABLE -> 0
        ProductStatus.SOLD -> 1
        ProductStatus.REMOVED -> 2
    }

/** The date on which this product last changed state. */
val ProductDetail.activityDate: LocalDate
    get() = soldOn ?: removedOn ?: createdAt

fun ProductSortOrder.comparator(): Comparator<Product> = when (this) {
    ProductSortOrder.OLDEST_FIRST ->
        compareBy<Product> { it.details.status.rank }
            .thenBy { it.details.createdAt }
            .thenBy { it.details.description.lowercase() }
            .thenBy { it.details.id }

    ProductSortOrder.NEWEST_FIRST ->
        compareBy<Product> { it.details.status.rank }
            .thenByDescending { it.details.createdAt }
            .thenBy { it.details.description.lowercase() }
            .thenBy { it.details.id }

    ProductSortOrder.RECENT_ACTIVITY ->
        compareByDescending<Product> { it.details.activityDate }
            .thenBy { it.details.description.lowercase() }
            .thenBy { it.details.id }
}