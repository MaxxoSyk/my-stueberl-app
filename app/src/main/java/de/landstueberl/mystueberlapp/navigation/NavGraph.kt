package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.GalleryRepository
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.repository.StatisticsRepository
import de.landstueberl.mystueberlapp.view.event.EventDetailScreen
import de.landstueberl.mystueberlapp.view.event.EventFormScreen
import de.landstueberl.mystueberlapp.view.event.EventsScreen
import de.landstueberl.mystueberlapp.view.gallery.GalleryScreen
import de.landstueberl.mystueberlapp.view.home.HomeScreen
import de.landstueberl.mystueberlapp.view.order.OrderDetailScreen
import de.landstueberl.mystueberlapp.view.order.OrdersScreen
import de.landstueberl.mystueberlapp.view.product.ProductDetailScreen
import de.landstueberl.mystueberlapp.view.product.ProductsScreen
import de.landstueberl.mystueberlapp.view.product.add.AddProductScreen
import de.landstueberl.mystueberlapp.view.product.edit.EditProductScreen
import de.landstueberl.mystueberlapp.view.statistics.StatisticsScreen
import de.landstueberl.mystueberlapp.viewmodel.event.EventDetailViewModel
import de.landstueberl.mystueberlapp.viewmodel.event.EventFormViewModel
import de.landstueberl.mystueberlapp.viewmodel.event.EventsViewModel
import de.landstueberl.mystueberlapp.viewmodel.gallery.GalleryViewModel
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.order.OrderDetailViewModel
import de.landstueberl.mystueberlapp.viewmodel.order.OrdersViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductDetailViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModel
import de.landstueberl.mystueberlapp.viewmodel.product.edit.EditProductViewModel
import de.landstueberl.mystueberlapp.viewmodel.statistics.StatisticsViewModel
import kotlinx.coroutines.CoroutineScope

private const val KEY_PRODUCT_SAVED = "product_saved"
private const val KEY_PRODUCT_UPDATED = "product_updated"
private const val KEY_ORDER_SAVED = "order_saved"
private const val KEY_EVENT_SAVED = "event_saved"

@Composable
fun NavGraph(
    navController: NavHostController,
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
    eventRepository: EventRepository,
    statisticsRepository: StatisticsRepository,
    galleryRepository: GalleryRepository,
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
                        orderRepository = orderRepository,
                        eventRepository = eventRepository,
                        statisticsRepository = statisticsRepository
                    )
                ),
                onNavigateToProducts = { navController.navigate(Screen.Products) },
                onNavigateToOrders = { navController.navigate(Screen.Orders) },
                onNavigateToEvents = { navController.navigate(Screen.Events) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics) },
                onNavigateToGallery = { navController.navigate(Screen.Gallery) }
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
                    factory = ProductsViewModel.factory(
                        repo = productRepository,
                        eventRepository = eventRepository,
                        imageStorage = imageStorage
                    )
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
                onNavigateToEvent = { eventId ->
                    navController.navigate(Screen.EventDetail(eventId))
                }
            )
        }

        composable<Screen.AddProduct> {
            AddProductScreen(
                viewModel = viewModel(
                    factory = AddProductViewModel.factory(
                        repo = productRepository,
                        eventRepository = eventRepository,
                        imageStorage = imageStorage
                    )
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
        composable<Screen.Statistics> {
            StatisticsScreen(
                viewModel = viewModel(
                    factory = StatisticsViewModel.factory(statisticsRepository)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Events ─────────────────────────────
        composable<Screen.Events> { backStackEntry ->
            val eventSaved by backStackEntry.savedStateHandle
                .getStateFlow(KEY_EVENT_SAVED, false)
                .collectAsStateWithLifecycle()

            EventsScreen(
                viewModel = viewModel(
                    factory = EventsViewModel.factory(eventRepository)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEvent = { eventId ->
                    navController.navigate(Screen.EventDetail(eventId))
                },
                onNavigateToNewEvent = {
                    navController.navigate(Screen.EventForm())
                },
                eventSaved = eventSaved,
                onEventSavedConsumed = {
                    backStackEntry.savedStateHandle[KEY_EVENT_SAVED] = false
                }
            )
        }

        composable<Screen.EventForm> {
            EventFormScreen(
                viewModel = viewModel(
                    factory = EventFormViewModel.factory(eventRepository)
                ),
                onNavigateBack = { eventSaved ->
                    if (eventSaved) {
                        // Signals whichever screen opened the form: the list now,
                        // the detail screen once editing exists.
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_EVENT_SAVED, true)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.EventDetail> { backStackEntry ->
            // Set by the form when an edit is saved.
            val eventSaved by backStackEntry.savedStateHandle
                .getStateFlow(KEY_EVENT_SAVED, false)
                .collectAsStateWithLifecycle()

            EventDetailScreen(
                viewModel = viewModel(
                    factory = EventDetailViewModel.factory(eventRepository, imageStorage)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { eventId ->
                    navController.navigate(Screen.EventForm(eventId))
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct(productId))
                },
                onNavigateToAddProduct = { eventId ->
                    navController.navigate(Screen.AddProduct(eventId = eventId))
                },
                eventSaved = eventSaved,
                onEventSavedConsumed = {
                    backStackEntry.savedStateHandle[KEY_EVENT_SAVED] = false
                }
            )
        }

        // ── Gallery ────────────────────────────
        composable<Screen.Gallery> {
            GalleryScreen(
                viewModel = viewModel(
                    factory = GalleryViewModel.factory(galleryRepository, imageStorage)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProduct = { productId ->
                    navController.navigate(Screen.ProductDetail(productId))
                }
            )
        }

        composable<Screen.ProductDetail> {
            ProductDetailScreen(
                viewModel = viewModel(
                    factory = ProductDetailViewModel.factory(productRepository, imageStorage)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}