package de.landstueberl.mystueberlapp.data.db.relation

import androidx.room.Embedded
import de.landstueberl.mystueberlapp.data.Money

/**
 * One material name (exact spelling) at one unit price, as aggregated by
 * [de.landstueberl.mystueberlapp.data.db.dao.ProductDao.getMaterialPriceUsages].
 */
data class MaterialPriceUsage(
    val name: String,
    @Embedded(prefix = "unit_") val unitPrice: Money,
    /** Highest row id with this name and price — a proxy for "last used". */
    val lastUsedId: Int,
    /** Number of products using it, where a whole batch counts once. */
    val usageCount: Int
)