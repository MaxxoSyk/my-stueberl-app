package de.landstueberl.mystueberlapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.navigation.NavGraph
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.ui.theme.MyStueberlAppTheme
import de.landstueberl.mystueberlapp.viewmodel.HomeViewModelFactory
import de.landstueberl.mystueberlapp.viewmodel.ProductsViewModelFactory

class HomeActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
                applicationContext,
                StueberlDatabase::class.java,
                "stueberl_database"
            ).fallbackToDestructiveMigration(true)
            .build()
    }

    private val productRepository by lazy {
        ProductRepository(database.productDao())
    }

    private val orderRepository by lazy {
        OrderRepository(
            orderDao = database.orderDao(),
            productDao = database.productDao()
        )
    }

    private val homeViewModelFactory by lazy {
        HomeViewModelFactory(
            productRepository = productRepository,
            orderRepository = orderRepository
        )
    }

    private val productsViewModelFactory by lazy {
        ProductsViewModelFactory(productRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyStueberlAppTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    homeViewModel = viewModel(factory = homeViewModelFactory),
                    productsViewModelFactory = productsViewModelFactory
                )
            }
        }
    }
}