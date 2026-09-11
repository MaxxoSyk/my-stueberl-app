package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.landstueberl.mystueberlapp.view.product.add.AddProductScreen
import de.landstueberl.mystueberlapp.view.home.HomeScreen
import de.landstueberl.mystueberlapp.view.product.ProductsScreen
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModelFactory
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    productsViewModelFactory: ProductsViewModelFactory,
    addProductViewModelFactory: AddProductViewModelFactory
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToProducts = {
                    navController.navigate(Screen.Products.route)
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }
        composable(Screen.Products.route) {
            val productSaved = it.savedStateHandle
                .getStateFlow("product_saved", false)
                .collectAsState()

            ProductsScreen(
                viewModel = viewModel(factory = productsViewModelFactory),
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct.route)
                },
                productSaved = productSaved.value,
                onProductSavedConsumed = {
                    it.savedStateHandle["product_saved"] = false
                }
            )
        }
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                viewModel = viewModel(factory = addProductViewModelFactory),
                onNavigateBack = { showSuccess ->
                    if (showSuccess) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("product_saved" , true)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}