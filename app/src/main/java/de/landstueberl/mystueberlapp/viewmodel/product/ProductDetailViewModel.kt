package de.landstueberl.mystueberlapp.viewmodel.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.groupByBatch
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class ProductDetailViewModel(
    repo: ProductRepository,
    private val imageStorage: ProductImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: Int = savedStateHandle.toRoute<Screen.ProductDetail>().productId

    /** What the screen should show. */
    sealed interface State {
        data object Loading : State

        /** The product was deleted while the screen was open, or never existed. */
        data object NotFound : State

        /** A single product, or its batch as a group. */
        data class Loaded(val item: ProductListItem) : State
    }

    val state: StateFlow<State> = repo
        .getProductWithBatchFlow(productId)
        .map { products ->
            products.groupByBatch().firstOrNull()
                ?.let { State.Loaded(it) }
                ?: State.NotFound
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), State.Loading)

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            repo: ProductRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<ProductDetailViewModel> {
                ProductDetailViewModel(
                    repo = repo,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}