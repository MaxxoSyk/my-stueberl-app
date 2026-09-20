package de.landstueberl.mystueberlapp

import android.app.Application
import android.util.Log
import androidx.room.Room
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    val productImageStorage by lazy { ProductImageStorage(applicationContext) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        cleanUpOrphanedImages()
    }

    /**
     * Removes image files with no database row — e.g. a picture chosen in the
     * form that was never saved.
     */
    private fun cleanUpOrphanedImages() {
        appScope.launch {
            try {
                val referenced = productRepository.getAllImageFileNames().toSet()
                productImageStorage.deleteOrphans(referenced)
                productImageStorage.clearCameraTemp()
            } catch (e: Exception) {
                Log.e("StueberlApplication", "Image cleanup failed", e)
            }
        }
    }
}