package de.landstueberl.mystueberlapp.viewmodel.product.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.landstueberl.mystueberlapp.repository.ProductRepository

class AddProductViewModelFactory(
    private val repository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}