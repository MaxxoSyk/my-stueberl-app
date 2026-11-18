package de.landstueberl.mystueberlapp

import android.app.Application
import android.util.Log
import androidx.room.Room
import de.landstueberl.mystueberlapp.data.db.StueberlDatabase
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.label.LabelPrinter
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.EventSaleRepository
import de.landstueberl.mystueberlapp.repository.GalleryRepository
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.repository.StatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.getValue

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
            db = database,
            orderDao = database.orderDao(),
            productDao = database.productDao()
        )
    }

    val statisticsRepository by lazy {
        StatisticsRepository(database.statisticsDao())
    }

    val galleryRepository by lazy {
        GalleryRepository(database.galleryDao())
    }

    val productImageStorage by lazy { ProductImageStorage(applicationContext) }

    val eventRepository by lazy { EventRepository(database, database.eventDao()) }

    val eventSaleRepository by lazy {
        EventSaleRepository(database, database.eventSaleDao())
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        cleanUpOrphanedImages()
        clearLabelCache()
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

    /** The last printed label sheet isn't needed after a restart. */
    private fun clearLabelCache() {
        appScope.launch {
            try {
                LabelPrinter.clearCache(applicationContext)
            } catch (e: Exception) {
                Log.e("StueberlApplication", "Label cache cleanup failed", e)
            }
        }
    }
}