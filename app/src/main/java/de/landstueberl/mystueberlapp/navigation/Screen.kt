package de.landstueberl.mystueberlapp.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data object Home : Screen

    @Serializable
    data object Products : Screen

    @Serializable
    data object Orders : Screen

    @Serializable
    data object Statistics : Screen

    /** [orderId] is set when the product is being created for an order. */
    @Serializable
    data class AddProduct(val orderId: Int? = null) : Screen

    @Serializable
    data class EditProduct(val productId: Int) : Screen

    /** [orderId] is null when creating a new order. */
    @Serializable
    data class OrderDetail(val orderId: Int? = null) : Screen

    /** Read-only view of one product, or its batch. Reached from the gallery. */
    @Serializable
    data class ProductDetail(val productId: Int) : Screen

    @Serializable
    data object Gallery : Screen

    @Serializable
    data object Events : Screen

    /** [eventId] is null when creating a new event. */
    @Serializable
    data class EventForm(val eventId: Int? = null) : Screen

    @Serializable
    data class EventDetail(val eventId: Int) : Screen
}