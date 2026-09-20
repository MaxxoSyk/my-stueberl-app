package de.landstueberl.mystueberlapp.viewmodel.product.add

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormValues
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormViewModel

class AddProductViewModel(
    repo: ProductRepository,
    imageStorage: ProductImageStorage
) : ProductFormViewModel(repo, imageStorage) {

    override suspend fun persist(values: ProductFormValues) {
        val product = Product(
            details = ProductDetail(
                id = 0,
                description = values.description,
                purchasePrice = values.purchasePrice,
                salesPrice = values.salesPrice,
                status = ProductStatus.AVAILABLE
            ),
            imageList = values.imageFileName
                ?.let { listOf(Image(productId = 0, fileName = it)) }
                ?: emptyList()
        )
        repo.upsertProduct(product)
    }

    companion object {
        fun factory(
            repo: ProductRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<AddProductViewModel> { AddProductViewModel(repo, imageStorage) }
        }
    }
}