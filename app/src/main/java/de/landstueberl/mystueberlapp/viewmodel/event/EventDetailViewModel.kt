package de.landstueberl.mystueberlapp.viewmodel.event

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.EventProducts
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.TillFigures
import de.landstueberl.mystueberlapp.data.allProducts
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.db.entity.EventSale
import de.landstueberl.mystueberlapp.data.groupByBatch
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.label.ProductLabel
import de.landstueberl.mystueberlapp.data.label.ProductLabelCode
import de.landstueberl.mystueberlapp.data.toEventProducts
import de.landstueberl.mystueberlapp.data.toTillFigures
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.EventSaleRepository
import de.landstueberl.mystueberlapp.viewmodel.common.AttachSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Loaded(val event: EventDetail) : EventDetailUiState
}

/** Specific reasons an action was refused, shown as a snackbar. */
enum class EventNotice { CLOSE_BLOCKED, REOPEN_REFUSED, DELETE_BLOCKED }

class EventDetailViewModel(
    private val repo: EventRepository,
    private val saleRepo: EventSaleRepository,
    private val imageStorage: ProductImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Int = savedStateHandle.toRoute<Screen.EventDetail>().eventId

    /** Set while deleting, so the screen doesn't flash "not found" on the way out. */
    private val isDeleting = MutableStateFlow(false)

    /** Observed live, so status changes and edits show up immediately. */
    val uiState: StateFlow<EventDetailUiState> =
        combine(repo.getEventFlow(eventId), isDeleting) { event, deleting ->
            when {
                deleting -> EventDetailUiState.Loading
                event == null -> EventDetailUiState.NotFound
                else -> EventDetailUiState.Loaded(event)
            }
        }
            .catch { e ->
                Log.e(TAG, "Failed to observe event $eventId", e)
                emit(EventDetailUiState.NotFound)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                EventDetailUiState.Loading
            )

    /**
     * The event's products, observed live: linking, unlinking, selling at the
     * till and selling elsewhere all show up immediately. Null while loading.
     */
    val products: StateFlow<EventProducts?> = combine(
        repo.getEventProductsFlow(eventId),
        saleRepo.getSalesFlow(eventId).map { sales -> sales.mapTo(HashSet()) { it.id } },
        // While preparing, the packing list grows from the top.
        repo.getEventFlow(eventId)
            .map { it?.status == EventStatus.IN_PREPARATION }
            .distinctUntilChanged()
    ) { list, saleIds, newestFirst ->
        list.toEventProducts(saleIds, newestFirst)
    }
        .catch { e ->
            Log.e(TAG, "Failed to observe products of event $eventId", e)
            emit(emptyList<Product>().toEventProducts())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // ── Attach Existing Products ───────────────
    /** Stand stock that can be taken to this event, grouped like the products list. */
    val attachableItems: StateFlow<List<ProductListItem>> =
        repo.getAttachableProductsFlow(eventId)
            .map { it.groupByBatch() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val attachSelection = AttachSelection()
    val selectedForAttach: StateFlow<Set<Int>> = attachSelection.selectedIds

    fun toggleAttachSelection(productId: Int) = attachSelection.toggle(productId)

    fun toggleBatchAttachSelection(productIds: List<Int>) = attachSelection.toggleBatch(productIds)

    fun clearAttachSelection() = attachSelection.clear()

    /**
     * Links the ticked products. The SQL guard skips any that were sold or
     * assigned elsewhere meanwhile, and links nothing unless still preparing.
     */
    fun attachSelectedProducts(onAttached: () -> Unit) {
        val ids = attachSelection.current.toList()
        if (ids.isEmpty() || currentEvent?.status != EventStatus.IN_PREPARATION) return

        launchWorking {
            repo.linkProducts(eventId, ids)
            attachSelection.clear()
            onAttached()
        }
    }

    private val currentEvent: EventDetail?
        get() = (uiState.value as? EventDetailUiState.Loaded)?.event

    // ── Label Printing ─────────────────────────
    /** Separate from the attach selection, so the two pickers never share ticks. */
    private val labelSelection = AttachSelection()
    val selectedForLabels: StateFlow<Set<Int>> = labelSelection.selectedIds

    fun toggleLabelSelection(productId: Int) = labelSelection.toggle(productId)

    fun toggleBatchLabelSelection(productIds: List<Int>) = labelSelection.toggleBatch(productIds)

    fun clearLabelSelection() = labelSelection.clear()

    /** Preselects every available product. Step 4 narrows this to unprinted ones. */
    fun selectAllForLabels() {
        labelSelection.replaceWith(availableProducts().map { it.details.id })
    }

    /**
     * Labels for the ticked products that are still available, in list order.
     * A product sold while the dialog was open is simply left out.
     */
    fun selectedLabels(): List<ProductLabel> {
        val selected = labelSelection.current
        return availableProducts()
            .filter { it.details.id in selected }
            .map { ProductLabel(ProductLabelCode(eventId, it.details.id), it.details.description) }
    }

    /** Observed live, so the dialog shows "Printed on …" right after confirming. */
    val labelPrintedOn: StateFlow<Map<Int, LocalDate>> = repo.getLabelPrintDatesFlow(eventId)
        .catch { e ->
            Log.e(TAG, "Failed to observe label prints of event $eventId", e)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    /** Used when the dialog opens: only products that don't have a label yet. */
    fun preselectUnprintedLabels() {
        val printed = labelPrintedOn.value
        labelSelection.replaceWith(
            availableProducts().map { it.details.id }.filter { it !in printed }
        )
    }

    /** Called only after the user confirmed the printout. */
    fun markLabelsPrinted(productIds: List<Int>) = launchWorking {
        repo.markLabelsPrinted(eventId, productIds)
    }

    private fun availableProducts(): List<Product> =
        products.value?.items.orEmpty().allProducts()

    // ── UI State ───────────────────────────────
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun resetErrorState() {
        _isError.value = false
    }

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    // ── Actions ────────────────────────────────
    /** PLANNED → IN_PREPARATION. Other transitions follow with their screens. */
    fun startPreparation() {
        if (currentEvent?.status != EventStatus.PLANNED) return
        launchWorking { repo.updateStatus(eventId, EventStatus.IN_PREPARATION) }
    }

    /** IN_PREPARATION → IN_PROGRESS. Needs at least one available product (guarded in SQL too). */
    fun startEvent() {
        if (currentEvent?.status != EventStatus.IN_PREPARATION) return
        if ((products.value?.availableCount ?: 0) == 0) return

        launchWorking {
            // A failed guard (e.g. the last product was sold meanwhile) shows the error snackbar.
            check(repo.startEvent(eventId)) { "Event $eventId could not be started" }
        }
    }

    /** IN_PROGRESS → DONE. Refused in SQL while the basket isn't empty. */
    fun closeEvent() {
        if (currentEvent?.status != EventStatus.IN_PROGRESS) return
        launchWorking {
            if (!repo.closeEvent(eventId)) _notice.value = EventNotice.CLOSE_BLOCKED
        }
    }

    /**
     * DONE → IN_PROGRESS while the last day hasn't passed. Refused in SQL if a
     * product was put into an order or another event meanwhile.
     */
    fun reopenEvent() {
        val event = currentEvent ?: return
        if (!event.canReopen(LocalDate.now())) return
        launchWorking {
            if (!repo.reopenEvent(eventId)) _notice.value = EventNotice.REOPEN_REFUSED
        }
    }

    /** Links stay as history; the products are free again. Guarded in SQL as well. */
    fun cancelEvent() {
        if (currentEvent?.status?.isCancellable != true) return
        launchWorking { repo.cancelEvent(eventId) }
    }

    /** Unlinks products; they stay in stock. Only while preparing (guarded in SQL too). */
    fun removeProducts(productIds: List<Int>) {
        if (currentEvent?.status != EventStatus.IN_PREPARATION) return
        launchWorking { repo.unlinkProducts(eventId, productIds) }
    }

    /**
     * Product links are removed by CASCADE; the products themselves stay.
     * Refused in SQL if the event has sales, e.g. one completed meanwhile.
     */
    fun deleteEvent(onDeleted: () -> Unit) = launchWorking {
        isDeleting.value = true
        val deleted = try {
            repo.deleteEvent(eventId)
        } catch (e: Exception) {
            isDeleting.value = false
            throw e
        }
        if (deleted) {
            onDeleted()
        } else {
            // Back to the screen instead of the spinner, with the reason.
            isDeleting.value = false
            _notice.value = EventNotice.DELETE_BLOCKED
        }
    }

    // ── Closing ────────────────────────────────
    /** Closing is blocked while products are still in the till's basket. */
    val basketCount: StateFlow<Int> = saleRepo.getBasketCountFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** An event with sales can't be deleted (guarded in SQL too). */
    val hasSales: StateFlow<Boolean> = saleRepo.hasSalesFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun notifyDeleteBlocked() {
        _notice.value = EventNotice.DELETE_BLOCKED
    }

    /** Revenue, items and customers for the close summary. */
    val figures: StateFlow<TillFigures> = saleRepo.getSalesFlow(eventId)
        .map { it.toTillFigures() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList<EventSale>().toTillFigures()
        )

    private val _notice = MutableStateFlow<EventNotice?>(null)
    val notice: StateFlow<EventNotice?> = _notice.asStateFlow()

    fun resetNotice() {
        _notice.value = null
    }

    private fun launchWorking(block: suspend () -> Unit) {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Event action failed", e)
                _isError.value = true
            } finally {
                _isWorking.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EventDetailViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            repo: EventRepository,
            saleRepo: EventSaleRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<EventDetailViewModel> {
                EventDetailViewModel(
                    repo = repo,
                    saleRepo = saleRepo,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}