package de.landstueberl.mystueberlapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import de.landstueberl.mystueberlapp.navigation.NavGraph
import de.landstueberl.mystueberlapp.ui.theme.MyStueberlAppTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyStueberlApplication

        setContent {
            MyStueberlAppTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    productRepository = app.productRepository,
                    orderRepository = app.orderRepository
                )
            }
        }
    }
}