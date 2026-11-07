package de.landstueberl.mystueberlapp.data

import androidx.room.Embedded

/** A previously used material name with up to [MAX_PRICE_SUGGESTIONS] price suggestions. */
data class MaterialSuggestion(
    val name: String,
    val prices: List<Money>
)

/**
 * One material name (exact spelling) at one unit price, as aggregated by
 * [ProductDao.getMaterialPriceUsages].
 */
data class MaterialPriceUsage(
    val name: String,
    @Embedded(prefix = "unit_") val unitPrice: Money,
    /** Highest row id with this name and price — a proxy for "last used". */
    val lastUsedId: Int,
    /** Number of products using it, where a whole batch counts once. */
    val usageCount: Int
)

private const val MAX_PRICE_SUGGESTIONS = 3

/**
 * Key for comparing material names: trimmed and case-insensitive.
 *
 * Kotlin's `lowercase()` folds umlauts ("Ö" → "ö"); SQLite's `NOCASE` does not,
 * which is why grouping happens here and not in SQL.
 */
fun materialKey(name: String): String = name.trim().lowercase()

/**
 * Groups [usages] into one suggestion per material, case-insensitively.
 *
 * Materials are ordered by total use, then alphabetically. Prices are the
 * [maxPrices] most recently used distinct non-zero prices, ordered by use.
 */
fun buildMaterialSuggestions(
    usages: List<MaterialPriceUsage>,
    maxPrices: Int = MAX_PRICE_SUGGESTIONS
): List<MaterialSuggestion> =
    usages
        .filter { it.name.isNotBlank() }
        .groupBy { materialKey(it.name) }
        .values
        .sortedWith(
            compareByDescending<List<MaterialPriceUsage>> { rows -> rows.sumOf { it.usageCount } }
                .thenBy { rows -> materialKey(rows.first().name) }
        )
        .map { rows ->
            MaterialSuggestion(
                // The most recently used spelling wins.
                name = rows.maxBy { it.lastUsedId }.name.trim(),
                prices = pricesFor(rows, maxPrices)
            )
        }

private fun pricesFor(rows: List<MaterialPriceUsage>, maxPrices: Int): List<Money> =
    rows
        .filter { it.unitPrice.amount.signum() > 0 }
        // Merges spellings: "Wolle" and "wolle" at 2.50 € are one price.
        // Money is always scale 2, so equals() is reliable here.
        .groupBy { it.unitPrice }
        .map { (price, same) ->
            PriceStat(
                price = price,
                lastUsedId = same.maxOf { it.lastUsedId },
                usageCount = same.sumOf { it.usageCount }
            )
        }
        // The last N distinct prices…
        .sortedByDescending { it.lastUsedId }
        .take(maxPrices)
        // …shown most used first, newer first on a tie.
        .sortedWith(
            compareByDescending<PriceStat> { it.usageCount }
                .thenByDescending { it.lastUsedId }
        )
        .map { it.price }

private data class PriceStat(val price: Money, val lastUsedId: Int, val usageCount: Int)