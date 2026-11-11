package de.landstueberl.mystueberlapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.landstueberl.mystueberlapp.data.EventDao
import de.landstueberl.mystueberlapp.data.GalleryDao
import de.landstueberl.mystueberlapp.data.OrderDao
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.StatisticsDao
import de.landstueberl.mystueberlapp.data.converter.DateConverters
import de.landstueberl.mystueberlapp.data.converter.MoneyConverters
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.db.entity.EventProductDetail
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail

@Database(
    entities = [
        ProductDetail::class,
        Image::class,
        OrderDetail::class,
        ProductMaterialDetail::class,
        EventDetail::class,
        EventProductDetail::class
    ],
    version = 15,
    exportSchema = true
)
@TypeConverters(
    MoneyConverters::class,
    DateConverters::class
)
abstract class StueberlDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun galleryDao(): GalleryDao
    abstract fun eventDao(): EventDao
}