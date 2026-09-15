package de.landstueberl.mystueberlapp.viewmodel.product.add

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormValues
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormViewModel

class AddProductViewModel(
    repo: ProductRepository
) : ProductFormViewModel(repo) {

    override suspend fun persist(values: ProductFormValues) {
        val product = Product(
            details = ProductDetail(
                id = 0,
                description = values.description,
                purchasePrice = values.purchasePrice,
                salesPrice = values.salesPrice,
                isSold = false,
                isRemoved = false
            ),
            imageList = emptyList()
        )
        repo.upsertProduct(product)
    }

    companion object {
        fun factory(repo: ProductRepository) = viewModelFactory {
            initializer { AddProductViewModel(repo) }
        }
    }
}