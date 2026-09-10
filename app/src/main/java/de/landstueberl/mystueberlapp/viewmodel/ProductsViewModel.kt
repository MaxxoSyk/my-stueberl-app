package de.landstueberl.mystueberlapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductsViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    // ── Products List ──────────────────────────
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    // ── Selection Mode ─────────────────────────
    private val _selectedProducts = MutableStateFlow<Set<Int>>(emptySet())
    val selectedProducts: StateFlow<Set<Int>> = _selectedProducts

    val isSelectionMode: Boolean
        get() = _selectedProducts.value.isNotEmpty()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _products.value = repo.getAllProducts()
        }
    }

    fun toggleSelection(productId: Int) {
        val current = _selectedProducts.value.toMutableSet()
        if (current.contains(productId)) {
            current.remove(productId)
        } else {
            current.add(productId)
        }
        _selectedProducts.value = current
    }

    fun clearSelection() {
        _selectedProducts.value = emptySet()
    }

    fun deleteSelectedProducts() {
        viewModelScope.launch {
            repo.deleteProductsByIds(_selectedProducts.value.toList())
            clearSelection()
            loadProducts()
        }
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            repo.upsertProduct(product)
            loadProducts()
        }
    }
}