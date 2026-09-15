package de.landstueberl.mystueberlapp.data.converter

import androidx.room.TypeConverter
import de.landstueberl.mystueberlapp.data.ProductStatus

class ProductStatusConverters {

    @TypeConverter
    fun fromProductStatus(status: ProductStatus): String = status.name

    @TypeConverter
    fun toProductStatus(value: String): ProductStatus = ProductStatus.valueOf(value)
}