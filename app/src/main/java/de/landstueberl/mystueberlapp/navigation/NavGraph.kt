package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.view.home.HomeScreen
import de.landstueberl.mystueberlapp.view.order.OrderDetailScreen
import de.landstueberl.mystueberlapp.view.order.OrdersScreen
import de.landstueberl.mystueberlapp.view.product.ProductsScreen
import de.landstueberl.mystueberlapp.view.product.add.AddProductScreen
import de.landstueberl.mystueberlapp.view.product.edit.EditProductScreen
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.order.OrderDetailViewModel
import de.landstueberl.mystueberlapp.viewmodel.order.OrdersViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.edit.EditProductViewModel
import kotlinx.coroutines.CoroutineScope

private const val KEY_PRODUCT_SAVED = "product_saved"
private const val KEY_PRODUCT_UPDATED = "product_updated"
private const val KEY_ORDER_SAVED = "order_saved"

@Composable
fun NavGraph(
    navController: NavHostController,
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
    imageStorage: ProductImageStorage,
    appScope: CoroutineScope
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        // ── Home ───────────────────────────────
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

        // ── Products ───────────────────────────
        composable<Screen.Products> { backStackEntry ->
            val productSaved by backStackEntry.savedStateHandle
                .getStateFlow(KEY_PRODUCT_SAVED, false)
                .collectAsStateWithLifecycle()

            val productUpdated by backStackEntry.savedStateHandle
                .getStateFlow(KEY_PRODUCT_UPDATED, false)
                .collectAsStateWithLifecycle()

            ProductsScreen(
                viewModel = viewModel(
                    factory = ProductsViewModel.factory(productRepository, imageStorage)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct()) },
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
                },
                onNavigateToOrder = { orderId ->
                    navController.navigate(Screen.OrderDetail(orderId))
                },
            )
        }

        composable<Screen.AddProduct> {
            AddProductScreen(
                viewModel = viewModel(
                    factory = AddProductViewModel.factory(productRepository, imageStorage)
                ),
                onNavigateBack = { showSuccess ->
                    if (showSuccess) {
                        // Signals whichever screen opened the form: the products
                        // list, or an order detail screen.
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
                    factory = EditProductViewModel.factory(productRepository, imageStorage)
                ),
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

        // ── Orders ─────────────────────────────
        composable<Screen.Orders> { backStackEntry ->
            val orderSaved by backStackEntry.savedStateHandle
                .getStateFlow(KEY_ORDER_SAVED, false)
                .collectAsStateWithLifecycle()

            OrdersScreen(
                viewModel = viewModel(
                    factory = OrdersViewModel.factory(orderRepository)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrder = { orderId ->
                    navController.navigate(Screen.OrderDetail(orderId))
                },
                onNavigateToNewOrder = {
                    navController.navigate(Screen.OrderDetail())
                },
                orderSaved = orderSaved,
                onOrderSavedConsumed = {
                    backStackEntry.savedStateHandle[KEY_ORDER_SAVED] = false
                }
            )
        }

        composable<Screen.OrderDetail> {
            OrderDetailScreen(
                viewModel = viewModel(
                    factory = OrderDetailViewModel.factory(
                        repo = orderRepository,
                        imageStorage = imageStorage,
                        appScope = appScope
                    )
                ),
                onNavigateBack = { orderSaved ->
                    if (orderSaved) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_ORDER_SAVED, true)
                    }
                    navController.popBackStack()
                },
                onNavigateToAddProduct = { orderId ->
                    navController.navigate(Screen.AddProduct(orderId = orderId))
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct(productId))
                }
            )
        }

        // ── Statistics ─────────────────────────
        // @todo not implemented — navigating here currently fails
    }
}