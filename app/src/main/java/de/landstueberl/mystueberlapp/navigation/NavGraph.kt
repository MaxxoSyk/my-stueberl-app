package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.view.home.HomeScreen
import de.landstueberl.mystueberlapp.view.product.ProductsScreen
import de.landstueberl.mystueberlapp.view.product.add.AddProductScreen
import de.landstueberl.mystueberlapp.view.product.edit.EditProductScreen
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModelFactory
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModelFactory
import de.landstueberl.mystueberlapp.viewmodel.product.edit.EditProductViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    productsViewModelFactory: ProductsViewModelFactory,
    addProductViewModelFactory: AddProductViewModelFactory,
    productRepository: ProductRepository
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

            val productUpdated = it.savedStateHandle
                .getStateFlow("product_updated", false)
                .collectAsState()

            ProductsScreen(
                viewModel = viewModel(factory = productsViewModelFactory),
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct.route)
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(
                        Screen.EditProduct.createRoute(productId)
                    )
                },
                productSaved = productSaved.value,
                onProductSavedConsumed = {
                    it.savedStateHandle["product_saved"] = false
                },
                productUpdated = productUpdated.value,
                onProductUpdatedConsumed = {
                    it.savedStateHandle["product_updated"] = false
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
        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: return@composable
            EditProductScreen(
                viewModel = viewModel(
                    factory = EditProductViewModelFactory(
                        repository = productRepository,
                        productId = productId
                    )
                ),
                onNavigateBack = { productUpdated ->
                    if (productUpdated) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("product_updated", true)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}