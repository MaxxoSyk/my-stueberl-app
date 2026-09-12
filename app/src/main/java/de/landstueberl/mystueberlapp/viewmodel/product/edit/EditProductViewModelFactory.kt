package de.landstueberl.mystueberlapp.viewmodel.product.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.landstueberl.mystueberlapp.repository.ProductRepository

class EditProductViewModelFactory(
    private val repository: ProductRepository,
    private val productId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProductViewModel(repository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}