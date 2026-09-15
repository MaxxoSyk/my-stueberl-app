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
    @Serializable
    data object AddProduct : Screen
    @Serializable
    data class EditProduct(val productId: Int) : Screen
}