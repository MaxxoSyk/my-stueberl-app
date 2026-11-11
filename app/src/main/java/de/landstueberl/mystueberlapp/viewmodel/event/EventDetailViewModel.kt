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
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.groupByBatch
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.toEventProducts
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.viewmodel.common.AttachSelection
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

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Loaded(val event: EventDetail) : EventDetailUiState
}

class EventDetailViewModel(
    private val repo: EventRepository,
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
     * The event's products, observed live: linking, unlinking and a product
     * being sold elsewhere all show up immediately. Null while loading.
     */
    val products: StateFlow<EventProducts?> = repo.getEventProductsFlow(eventId)
        .map { it.toEventProducts() }
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

    /** Product links are removed by CASCADE; the products themselves stay. */
    fun deleteEvent(onDeleted: () -> Unit) = launchWorking {
        isDeleting.value = true
        try {
            repo.deleteEvent(eventId)
        } catch (e: Exception) {
            isDeleting.value = false
            throw e
        }
        onDeleted()
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
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<EventDetailViewModel> {
                EventDetailViewModel(
                    repo = repo,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}