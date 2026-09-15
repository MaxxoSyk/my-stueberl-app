package de.landstueberl.mystueberlapp

import android.app.Application
import androidx.room.Room
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository

class MyStueberlApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            StueberlDatabase::class.java,
            "stueberl_database"
        ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    val productRepository by lazy { ProductRepository(database.productDao()) }

    val orderRepository by lazy {
        OrderRepository(
            orderDao = database.orderDao(),
            productDao = database.productDao()
        )
    }
}