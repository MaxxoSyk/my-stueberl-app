package de.landstueberl.mystueberlapp.data

import androidx.annotation.StringRes
import de.landstueberl.mystueberlapp.R

@get:StringRes
val OrderStatus.labelRes: Int
    get() = when (this) {
        OrderStatus.OPEN -> R.string.order_status_open
        OrderStatus.RESOLVED -> R.string.order_status_resolved
        OrderStatus.CANCELLED -> R.string.order_status_cancelled
    }

/**
 * NOTE: constant names are embedded as SQL string literals in OrderDao.
 * Renaming one will not break compilation — it will silently make those
 * queries match nothing.
 */
enum class OrderStatus {
    OPEN,
    RESOLVED,
    CANCELLED
}