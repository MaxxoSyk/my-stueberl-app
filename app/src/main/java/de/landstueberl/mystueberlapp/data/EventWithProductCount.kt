package de.landstueberl.mystueberlapp.data

import androidx.room.Embedded
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail

/** An event with the number of products linked to it, for the list cards. */
data class EventWithProductCount(
    @Embedded val event: EventDetail,
    val productCount: Int
)