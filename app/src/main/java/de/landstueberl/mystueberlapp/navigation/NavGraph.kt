package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.view.home.HomeScreen
import de.landstueberl.mystueberlapp.view.product.ProductsScreen
import de.landstueberl.mystueberlapp.view.product.add.AddProductScreen
import de.landstueberl.mystueberlapp.view.product.edit.EditProductScreen
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.edit.EditProductViewModel

private const val KEY_PRODUCT_SAVED = "product_saved"
private const val KEY_PRODUCT_UPDATED = "product_updated"

@Composable
fun NavGraph(
    navController: NavHostController,
    productRepository: ProductRepository,
    orderRepository: OrderRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> {
            HomeScreen(
                viewModel = viewModel(
                    factory = HomeViewModel.factory(
                        productRepository = productRepository,
                        orderRepository = orderRepository
                    )
                ),
                onNavigateToProducts = { navController.navigate(Screen.Products) },
                onNavigateToOrders = { navController.navigate(Screen.Orders) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics) }
            )
        }

        composable<Screen.Products> { backStackEntry ->
            val productSaved by backStackEntry.savedStateHandle
                .getStateFlow(KEY_PRODUCT_SAVED, false)
                .collectAsStateWithLifecycle()

            val productUpdated by backStackEntry.savedStateHandle
                .getStateFlow(KEY_PRODUCT_UPDATED, false)
                .collectAsStateWithLifecycle()

            ProductsScreen(
                viewModel = viewModel(
                    factory = ProductsViewModel.factory(productRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct) },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct(productId))
                },
                productSaved = productSaved,
                onProductSavedConsumed = {
                    backStackEntry.savedStateHandle[KEY_PRODUCT_SAVED] = false
                },
                productUpdated = productUpdated,
                onProductUpdatedConsumed = {
                    backStackEntry.savedStateHandle[KEY_PRODUCT_UPDATED] = false
                }
            )
        }

        composable<Screen.AddProduct> {
            AddProductScreen(
                viewModel = viewModel(
                    factory = AddProductViewModel.factory(productRepository)),
                onNavigateBack = { showSuccess ->
                    if (showSuccess) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_PRODUCT_SAVED, true)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.EditProduct> {
            EditProductScreen(
                viewModel = viewModel(
                    factory = EditProductViewModel.factory(productRepository)),
                onNavigateBack = { productUpdated ->
                    if (productUpdated) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_PRODUCT_UPDATED, true)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}