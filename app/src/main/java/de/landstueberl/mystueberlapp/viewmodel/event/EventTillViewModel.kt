package de.landstueberl.mystueberlapp.viewmodel.event

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.BasketAddResult
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.data.SaleResult
import de.landstueberl.mystueberlapp.data.TillFigures
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.groupByBatch
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.salesTotal
import de.landstueberl.mystueberlapp.data.toTillFigures
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.EventSaleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** One-shot messages for the till's snackbar. */
sealed interface TillMessage {
    /** Adding didn't work; the reason is shown to the user. */
    data class NotAdded(val reason: BasketAddResult) : TillMessage
    /** saleId keeps two sales with the same total apart, so both are shown. */
    data class Sold(val saleId: Int, val total: Money) : TillMessage
    /** Nothing was sold: the basket differed from what was shown. */
    data object BasketChanged : TillMessage
    data object SaleNotRunning : TillMessage
    data object Failed : TillMessage
}

class EventTillViewModel(
    eventRepo: EventRepository,
    private val saleRepo: EventSaleRepository,
    private val imageStorage: ProductImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Int = savedStateHandle.toRoute<Screen.EventTill>().eventId

    /** Null while loading or if the event was deleted. */
    val event: StateFlow<EventDetail?> = eventRepo.getEventFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Only a running event can sell; the screen disables the till otherwise. */
    val isRunning: StateFlow<Boolean> = event
        .map { it?.status == EventStatus.IN_PROGRESS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    // ── Basket ─────────────────────────────────
    /** In the order the items were added. Saved in the DB, so it survives the app closing. */
    val basket: StateFlow<List<Product>> = saleRepo.getBasketFlow(eventId)
        .catch { e ->
            Log.e(TAG, "Failed to observe basket of event $eventId", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val basketTotal: StateFlow<Money> = basket
        .map { it.salesTotal() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            PriceInput.zeroMoney()
        )

    // ── Header Figures ─────────────────────────
    val figures: StateFlow<TillFigures> = saleRepo.getSalesFlow(eventId)
        .map { it.toTillFigures() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList<de.landstueberl.mystueberlapp.data.db.entity.EventSale>().toTillFigures()
        )

    // ── Adding by Hand ─────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Event products that can still go into the basket, filtered by the search
     * and grouped by batch. A batch row adds its next member, so identical
     * items don't need one row each.
     */
    val addableItems: StateFlow<List<ProductListItem>> = combine(
        eventRepo.getEventProductsFlow(eventId),
        basket,
        _searchQuery
    ) { products, basketItems, query ->
        val inBasket = basketItems.mapTo(HashSet()) { it.details.id }
        val q = query.trim()
        products
            .filter { product ->
                product.details.status == ProductStatus.AVAILABLE &&
                        product.details.id !in inBasket &&
                        (q.isEmpty() || product.details.description.contains(q, ignoreCase = true))
            }
            .groupByBatch()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // ── UI State ───────────────────────────────
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _message = MutableStateFlow<TillMessage?>(null)
    val message: StateFlow<TillMessage?> = _message.asStateFlow()

    fun resetMessage() {
        _message.value = null
    }

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    // ── Actions ────────────────────────────────
    /** All checks run in the repository's transaction; a refusal becomes a message. */
    fun addToBasket(productId: Int) = launchWorking {
        val result = saleRepo.addToBasket(eventId, productId)
        if (result != BasketAddResult.Added) {
            _message.value = TillMessage.NotAdded(result)
        }
    }

    fun removeFromBasket(productId: Int) = launchWorking {
        saleRepo.removeFromBasket(eventId, listOf(productId))
    }

    fun clearBasket() = launchWorking {
        saleRepo.clearBasket(eventId)
    }

    /**
     * Sells exactly the products the customer saw. [onFinished] runs for every
     * result, so the panel closes; on a database error it stays open.
     */
    fun completeSale(expectedProductIds: Set<Int>, onFinished: () -> Unit) = launchWorking {
        when (val result = saleRepo.completeSale(eventId, expectedProductIds)) {
            is SaleResult.Completed ->
                _message.value = TillMessage.Sold(result.sale.id, result.sale.total)
            SaleResult.BasketChanged -> _message.value = TillMessage.BasketChanged
            SaleResult.EventNotRunning -> _message.value = TillMessage.SaleNotRunning
            // Can't happen from the panel (it needs items); nothing to report.
            SaleResult.EmptyBasket -> Unit
        }
        onFinished()
    }

    /**
     * Ignores a tap while the previous action is still running. A basket write
     * takes milliseconds, and it keeps two quick taps on a batch row from
     * both picking the same member.
     */
    private fun launchWorking(block: suspend () -> Unit) {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Till action failed", e)
                _message.value = TillMessage.Failed
            } finally {
                _isWorking.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EventTillViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            eventRepository: EventRepository,
            eventSaleRepository: EventSaleRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<EventTillViewModel> {
                EventTillViewModel(
                    eventRepo = eventRepository,
                    saleRepo = eventSaleRepository,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}