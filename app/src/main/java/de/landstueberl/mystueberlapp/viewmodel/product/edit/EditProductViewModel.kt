package de.landstueberl.mystueberlapp.viewmodel.product.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormValues
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class EditProductViewModel(
    repo: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ProductFormViewModel(repo) {

    private val productId: Int = savedStateHandle.toRoute<Screen.EditProduct>().productId
    private var loadedProduct: Product? = null

    // ── Read Only Fields (edit-only) ───────────
    private val _createdAt = MutableStateFlow<LocalDate?>(null)
    val createdAt: StateFlow<LocalDate?> = _createdAt.asStateFlow()

    private val _soldOn = MutableStateFlow<LocalDate?>(null)
    val soldOn: StateFlow<LocalDate?> = _soldOn.asStateFlow()

    private val _removedOn = MutableStateFlow<LocalDate?>(null)
    val removedOn: StateFlow<LocalDate?> = _removedOn.asStateFlow()

    private val _status = MutableStateFlow(ProductStatus.AVAILABLE)
    val status: StateFlow<ProductStatus> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                val product = repo.getProductById(productId)
                loadedProduct = product

                val productDetails = product.details

                // Prefill the inherited form fields
                _description.value = productDetails.description
                _purchasePrice.value = productDetails.purchasePrice?.amount?.toPlainString() ?: ""
                _salesPrice.value = productDetails.salesPrice?.amount?.toPlainString() ?: ""
                _currency.value = productDetails.purchasePrice?.currency
                    ?: productDetails.salesPrice?.currency
                            ?: DEFAULT_CURRENCY

                // Edit-only read-only fields
                _createdAt.value = productDetails.createdAt
                _soldOn.value = productDetails.soldOn
                _removedOn.value = productDetails.removedOn
                _status.value = productDetails.status
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load product $productId", e)
                _loadFailed.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        loadProduct()
    }

    override suspend fun persist(values: ProductFormValues) {
        val existing = loadedProduct ?: repo.getProductById(productId)

        val updated = Product(
            details = existing.details.copy(
                description = values.description,
                purchasePrice = values.purchasePrice,
                salesPrice = values.salesPrice
            ),
            imageList = existing.imageList
        )

        repo.upsertProduct(updated)
        loadedProduct = updated
    }

    companion object {
        private const val TAG = "EditProductViewModel"

        fun factory(repo: ProductRepository) = viewModelFactory {
            initializer<EditProductViewModel> {
                EditProductViewModel(
                    repo = repo,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}