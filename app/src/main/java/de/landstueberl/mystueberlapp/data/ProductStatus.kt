package de.landstueberl.mystueberlapp.data

/**
 * NOTE: [ProductStatus] constant names are embedded as SQL string literals in
 * ProductDao ('AVAILABLE', 'SOLD', 'REMOVED'). Renaming a constant will not
 * break compilation — it will silently make those queries match nothing.
 */
enum class ProductStatus {
    AVAILABLE,
    SOLD,
    REMOVED
}