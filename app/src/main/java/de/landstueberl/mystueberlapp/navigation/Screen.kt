package de.landstueberl.mystueberlapp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Products : Screen("products")
    object Orders : Screen("orders")
    object Statistics : Screen("statistics")
    object AddProduct : Screen("add_product")
}