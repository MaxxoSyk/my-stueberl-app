package de.landstueberl.mystueberlapp.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Emits today's date, and again each midnight while collected.
 *
 * Collected through `stateIn(WhileSubscribed)`, it stops in the background and
 * emits the current date again on return, so a delay stretched by deep sleep
 * can't leave a stale date on screen.
 */
fun todayFlow(clock: Clock = Clock.systemDefaultZone()): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone)
        val millis = Duration.between(ZonedDateTime.now(clock), nextMidnight).toMillis()
        delay(millis.coerceAtLeast(1).milliseconds)
    }
}.distinctUntilChanged()