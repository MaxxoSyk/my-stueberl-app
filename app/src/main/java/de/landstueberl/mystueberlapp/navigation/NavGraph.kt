package de.landstueberl.mystueberlapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.landstueberl.mystueberlapp.view.HomeScreen
import de.landstueberl.mystueberlapp.view.ProductsScreen
import de.landstueberl.mystueberlapp.viewmodel.HomeViewModel
import de.landstueberl.mystueberlapp.viewmodel.ProductsViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    productsViewModelFactory: ProductsViewModelFactory
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
            ProductsScreen(
                viewModel = viewModel(factory = productsViewModelFactory),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}