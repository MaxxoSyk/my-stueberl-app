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
}