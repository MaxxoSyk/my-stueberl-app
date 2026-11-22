package de.landstueberl.mystueberlapp.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Shown instead of private figures (revenue etc.) while they're hidden. */
const val HIDDEN_VALUE = "••••"

/** How long figures stay visible after tapping to reveal them. */
const val REVEAL_MILLIS = 5_000L

/**
 * `true` while revealed; switches back to `false` by itself after
 * [REVEAL_MILLIS]. Plain remember, so it starts hidden whenever the screen
 * is opened again.
 */
@Composable
fun rememberTimedReveal(): MutableState<Boolean> {
    val revealed = remember { mutableStateOf(false) }
    LaunchedEffect(revealed.value) {
        if (revealed.value) {
            delay(REVEAL_MILLIS.milliseconds)
            revealed.value = false
        }
    }
    return revealed
}