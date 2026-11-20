package de.landstueberl.mystueberlapp.data.label

/** A product's identifier at one event, as read from the event–product link. */
data class ProductLabelIdentifier(
    val productId: Int,
    val labelIdentifier: String
)