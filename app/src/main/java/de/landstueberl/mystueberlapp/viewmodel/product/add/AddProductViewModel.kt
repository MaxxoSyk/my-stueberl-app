package de.landstueberl.mystueberlapp.viewmodel.product.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormValues
import de.landstueberl.mystueberlapp.viewmodel.product.ProductFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddProductViewModel(
    repo: ProductRepository,
    imageStorage: ProductImageStorage,
    savedStateHandle: SavedStateHandle
) : ProductFormViewModel(repo, imageStorage) {

    private val orderId: Int? = savedStateHandle.toRoute<Screen.AddProduct>().orderId

    /** Batch creation is sales-area only for now. */
    val isForOrder: Boolean = orderId != null

    // ── Quantity ───────────────────────────────
    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    fun onQuantityChange(value: Int) {
        _quantity.value = value.coerceIn(MIN_QUANTITY, MAX_QUANTITY)
    }

    fun incrementQuantity() = onQuantityChange(_quantity.value + 1)

    fun decrementQuantity() = onQuantityChange(_quantity.value - 1)

    override suspend fun persist(values: ProductFormValues) {
        val detail = ProductDetail(
            id = 0,
            description = values.description,
            purchasePrice = values.purchasePrice,
            salesPrice = values.salesPrice,
            status = ProductStatus.AVAILABLE,
            orderId = orderId
        )

        val count = if (isForOrder) 1 else _quantity.value

        if (count > 1) {
            repo.insertProductBatch(
                detail = detail,
                quantity = count,
                imageFileName = values.imageFileName
            )
        } else {
            // Single products stay out of the batch path: no batchId, and the
            // regular upsert keeps image syncing consistent with editing.
            repo.upsertProduct(
                Product(
                    details = detail,
                    imageList = values.imageFileName
                        ?.let { listOf(Image(productId = 0, fileName = it)) }
                        ?: emptyList()
                )
            )
        }
    }

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 50

        fun factory(
            repo: ProductRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<AddProductViewModel> {
                AddProductViewModel(
                    repo = repo,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}