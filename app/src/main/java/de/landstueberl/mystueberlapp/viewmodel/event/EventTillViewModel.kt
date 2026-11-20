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
import de.landstueberl.mystueberlapp.data.BasketLine
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.data.SaleResult
import de.landstueberl.mystueberlapp.data.SaleWithProducts
import de.landstueberl.mystueberlapp.data.TillFigures
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.groupByBatch
import de.landstueberl.mystueberlapp.data.groupSales
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.label.BatchLabelCode
import de.landstueberl.mystueberlapp.data.label.LabelCode
import de.landstueberl.mystueberlapp.data.label.ProductLabelCode
import de.landstueberl.mystueberlapp.data.salesTotal
import de.landstueberl.mystueberlapp.data.toBasketLines
import de.landstueberl.mystueberlapp.data.toTillFigures
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.EventSaleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** One-shot messages for the till: a snackbar, plus a vibration for scans and sales. */
sealed interface TillMessage {
    /** A scanned product went into the basket. */
    data class Added(val description: String) : TillMessage
    /** Adding didn't work; the reason is shown to the user. */
    data class NotAdded(val reason: BasketAddResult) : TillMessage
    /** Not one of our labels. */
    data object ForeignCode : TillMessage
    /** One of our labels, but for another event. */
    data object OtherEvent : TillMessage
    /** Module missing (no internet yet) or no Play Services. */
    data object ScannerUnavailable : TillMessage
    data class Sold(val saleId: Int, val total: Money) : TillMessage
    data object BasketChanged : TillMessage
    data object SaleNotRunning : TillMessage
    data object Failed : TillMessage
    /** A sale was undone; its products are available again. */
    data class Undone(val saleId: Int, val total: Money) : TillMessage
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

    /** The basket for display: batch members grouped into one row with a counter. */
    val basketLines: StateFlow<List<BasketLine>> = basket
        .map { it.toBasketLines() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // ── Header Figures ─────────────────────────
    val figures: StateFlow<TillFigures> = saleRepo.getSalesFlow(eventId)
        .map { it.toTillFigures() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList<de.landstueberl.mystueberlapp.data.db.entity.EventSale>().toTillFigures()
        )

    // ── Recent Sales ───────────────────────────
    /** Newest first, each with its products. */
    val recentSales: StateFlow<List<SaleWithProducts>> = combine(
        saleRepo.getSalesFlow(eventId),
        saleRepo.getSoldProductsFlow(eventId),
        ::groupSales
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** Only while the event is running; the repository checks that too. */
    fun undoSale(saleId: Int) = launchWorking {
        val sale = recentSales.value.firstOrNull { it.sale.id == saleId }?.sale
            ?: return@launchWorking
        if (saleRepo.undoSale(sale.id)) {
            send(TillMessage.Undone(sale.id, sale.total))
        } else {
            send(TillMessage.SaleNotRunning)
        }
    }

    // ── Adding by Hand ─────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val eventProducts: StateFlow<List<Product>> = eventRepo.getEventProductsFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** Batches with at least one more member available, so "+" can stay enabled. */
    val batchesWithMore: StateFlow<Set<Int>> = combine(eventProducts, basket) { products, basketItems ->
        val inBasket = basketItems.mapTo(HashSet()) { it.details.id }
        products
            .filter { it.details.status == ProductStatus.AVAILABLE && it.details.id !in inBasket }
            .mapNotNullTo(HashSet()) { it.details.batchId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    /** "+" on a batch row: the next available member of the batch. */
    fun addNextOfBatch(batchId: Int) = launchWorking {
        val result = saleRepo.addNextOfBatch(eventId, batchId)
        if (result != BasketAddResult.Added) send(TillMessage.NotAdded(result))
    }

    /** "−" on a batch row: puts the newest member back (the basket is oldest first). */
    fun removeNewestOfBatch(batchId: Int) {
        val newest = basket.value.lastOrNull { it.details.batchId == batchId } ?: return
        removeFromBasket(newest.details.id)
    }

    /**
     * Event products that can still go into the basket, filtered by the search
     * and grouped by batch. A batch row adds its next member, so identical
     * items don't need one row each.
     */
    val addableItems: StateFlow<List<ProductListItem>> = combine(
        eventProducts,
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

    private val _messages = Channel<TillMessage>(Channel.BUFFERED)

    /** Every message is delivered once, even two identical ones in a row. */
    val messages: Flow<TillMessage> = _messages.receiveAsFlow()

    private fun send(message: TillMessage) {
        _messages.trySend(message)
    }

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    // ── Actions ────────────────────────────────
    /** All checks run in the repository's transaction; a refusal becomes a message. */
    fun addToBasket(productId: Int) = launchWorking {
        val result = saleRepo.addToBasket(eventId, productId)
        if (result != BasketAddResult.Added) {
            send(TillMessage.NotAdded(result))
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
                send(TillMessage.Sold(result.sale.id, result.sale.total))
            SaleResult.BasketChanged -> send(TillMessage.BasketChanged)
            SaleResult.EventNotRunning -> send(TillMessage.SaleNotRunning)
            // Can't happen from the panel (it needs items); nothing to report.
            SaleResult.EmptyBasket -> Unit
        }
        onFinished()
    }

    // ── Scanning ───────────────────────────────
    /** Checks the label, then adds the product, or the next member of a batch. */
    fun onScanned(raw: String) {
        val code = LabelCode.decode(raw)
        when {
            code == null -> send(TillMessage.ForeignCode)
            code.eventId != eventId -> send(TillMessage.OtherEvent)
            code is ProductLabelCode -> launchCatching {
                when (val result = saleRepo.addToBasket(eventId, code.productId)) {
                    BasketAddResult.Added -> send(TillMessage.Added(descriptionOf(code.productId)))
                    else -> send(TillMessage.NotAdded(result))
                }
            }
            code is BatchLabelCode -> launchCatching {
                when (val result = saleRepo.addNextOfBatch(eventId, code.batchId)) {
                    BasketAddResult.Added -> send(TillMessage.Added(batchDescriptionOf(code.batchId)))
                    else -> send(TillMessage.NotAdded(result))
                }
            }
        }
    }

    private fun batchDescriptionOf(batchId: Int): String =
        eventProducts.value.firstOrNull { it.details.batchId == batchId }?.details?.description.orEmpty()

    fun onScannerFailed(e: Exception) {
        Log.w(TAG, "Scanner failed", e)
        send(TillMessage.ScannerUnavailable)
    }

    private fun descriptionOf(productId: Int): String =
        eventProducts.value.firstOrNull { it.details.id == productId }?.details?.description.orEmpty()

    /** Ignores a tap while the previous action runs (keeps quick taps on a batch row apart). */
    private fun launchWorking(block: suspend () -> Unit) {
        if (_isWorking.value) return
        launchCatching {
            _isWorking.value = true
            try {
                block()
            } finally {
                _isWorking.value = false
            }
        }
    }

    /** No tap guard: scans arrive one at a time and must never be dropped silently. */
    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Till action failed", e)
                send(TillMessage.Failed)
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