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
        ).apply {
            if (BuildConfig.DEBUG) {
                // Development only: wipe and recreate when the schema changes.
                // Release builds must have a real migration path instead.
                fallbackToDestructiveMigration(dropAllTables = true)
            }
        }.build()
    }

    val productRepository by lazy { ProductRepository(database.productDao()) }

    val orderRepository by lazy {
        OrderRepository(
            orderDao = database.orderDao(),
            productDao = database.productDao()
        )
    }
}