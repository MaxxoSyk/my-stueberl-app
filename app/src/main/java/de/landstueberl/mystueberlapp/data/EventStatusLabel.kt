package de.landstueberl.mystueberlapp.data

import androidx.annotation.StringRes
import de.landstueberl.mystueberlapp.R

@get:StringRes
val EventStatus.labelRes: Int
    get() = when (this) {
        EventStatus.PLANNED -> R.string.event_status_planned
        EventStatus.IN_PREPARATION -> R.string.event_status_in_preparation
        EventStatus.IN_PROGRESS -> R.string.event_status_in_progress
        EventStatus.DONE -> R.string.event_status_done
        EventStatus.CANCELLED -> R.string.event_status_cancelled
    }