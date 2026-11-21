package de.landstueberl.mystueberlapp.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.model.event.EventStatus
import de.landstueberl.mystueberlapp.data.model.event.NextEvent
import de.landstueberl.mystueberlapp.data.model.event.eventTiming
import de.landstueberl.mystueberlapp.data.model.statistics.StatisticsTileFigures
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.repository.StatisticsRepository
import de.landstueberl.mystueberlapp.util.todayFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository,
    private val eventRepo: EventRepository,
    private val statisticsRepo: StatisticsRepository
) : ViewModel() {
    /** Sales-area products still available. */
    val availableSalesAreaCount: StateFlow<Int> = productRepo
        .getAvailableSalesAreaCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** Products made for orders that haven't been resolved yet. */
    val pendingOrderProductsCount: StateFlow<Int> = productRepo
        .getPendingOrderProductsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** All products created this year, both sources. */
    val totalProductsCurrentYear: StateFlow<Int> = productRepo
        .getTotalProductsForYearFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    // ── Orders ─────────────────────────────────
    /** Orders still to be fulfilled. */
    val openOrderCount: StateFlow<Int> = orderRepo
        .getOpenOrderCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** The open order with the nearest due date, or null if there are none. */
    val nextDueOrder: StateFlow<OrderDetail?> = orderRepo
        .getNextDueOrderFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** All orders created this year, whatever their status. */
    val totalOrdersCurrentYear: StateFlow<Int> = orderRepo
        .getTotalOrdersForYearFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    // ── Statistics ─────────────────────────────
    val statisticsTileFigures: StateFlow<StatisticsTileFigures?> = statisticsRepo
        .getTileFiguresFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // ── Events ─────────────────────────────────
    /** Shared, so the tile and isEventRunning don't run the queries twice. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val nextEventFlow: SharedFlow<NextEvent?> = todayFlow()
        .flatMapLatest { today ->
            eventRepo.getNextEventFlow(today).flatMapLatest { event ->
                if (event == null) {
                    flowOf(null)
                } else {
                    eventRepo.getAvailableProductCountFlow(event.id).map { count ->
                        NextEvent(
                            event = event,
                            timing = eventTiming(event, today),
                            availableProductCount = count
                        )
                    }
                }
            }
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val nextEvent: StateFlow<NextEvent?> = nextEventFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /**
     * Whether an event is running, or null while still loading. The statistics
     * tile stays hidden while null, so the figures never flash up at a market.
     */
    val isEventRunning: StateFlow<Boolean?> = nextEventFlow
        .map { it?.event?.status == EventStatus.IN_PROGRESS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            productRepository: ProductRepository,
            orderRepository: OrderRepository,
            eventRepository: EventRepository,
            statisticsRepository: StatisticsRepository
        ) = viewModelFactory {
            initializer<HomeViewModel> {
                HomeViewModel(
                    productRepo = productRepository,
                    orderRepo = orderRepository,
                    eventRepo = eventRepository,
                    statisticsRepo = statisticsRepository
                )
            }
        }
    }
}