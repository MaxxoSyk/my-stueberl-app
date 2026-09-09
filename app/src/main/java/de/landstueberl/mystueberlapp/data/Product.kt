package de.landstueberl.mystueberlapp.data

import androidx.room.Embedded
import androidx.room.Relation
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail

data class Product(
    @Embedded val details: ProductDetail,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val imageList: List<Image>
)