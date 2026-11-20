package de.landstueberl.mystueberlapp.data.db.relation

/** A product's identifier at one event, as read from the event–product link. */
data class ProductLabelIdentifier(
    val productId: Int,
    val labelIdentifier: String
)