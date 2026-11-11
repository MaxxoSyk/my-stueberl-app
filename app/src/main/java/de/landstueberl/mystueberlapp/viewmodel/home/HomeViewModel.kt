package de.landstueberl.mystueberlapp.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.NextEvent
import de.landstueberl.mystueberlapp.data.StatisticsTileFigures
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.eventTiming
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import de.landstueberl.mystueberlapp.repository.StatisticsRepository
import de.landstueberl.mystueberlapp.util.todayFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /**
     * The nearest event that isn't over yet (cancelled ones skipped), with the
     * number of available products assigned to it. Null while loading or when
     * there is none; the tile shows "–" for both.
     *
     * Re-queried at midnight, so a finished event drops off on its own. The
     * count follows whichever event is next, and updates live as products are
     * assigned, removed or sold.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nextEvent: StateFlow<NextEvent?> = todayFlow()
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