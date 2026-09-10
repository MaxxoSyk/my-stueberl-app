package de.landstueberl.mystueberlapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.landstueberl.mystueberlapp.data.OrderDao
import de.landstueberl.mystueberlapp.data.ProductDao
import de.landstueberl.mystueberlapp.data.converter.MoneyConverters
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail

@Database(
    entities = [
        ProductDetail::class,
        Image::class,
        OrderDetail::class],
    version = 1
)
@TypeConverters(MoneyConverters::class)
abstract class StueberlDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
}