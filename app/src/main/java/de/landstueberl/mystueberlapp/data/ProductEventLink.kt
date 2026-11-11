package de.landstueberl.mystueberlapp.data

/** A product and the active event it's currently assigned to. */
data class ProductEventLink(
    val productId: Int,
    val eventId: Int
)