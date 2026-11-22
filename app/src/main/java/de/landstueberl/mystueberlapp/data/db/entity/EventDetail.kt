package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.landstueberl.mystueberlapp.data.model.common.Money
import de.landstueberl.mystueberlapp.data.model.event.EventStatus
import java.time.LocalDate

/** A sale event, e.g. a market, that products are taken to. */
@Entity
data class EventDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val location: String? = null,
    val eventDate: LocalDate,
    /**
     * Last day of a multi-day event, or null for a one-day event. Must not be
     * before [eventDate]. This is checked in the form rather than here, because
     * a throwing constructor would crash every read of a bad row.
     */
    val endDate: LocalDate? = null,
    /** Null means "not entered yet", as opposed to a free stand (0 €). */
    @Embedded(prefix = "stand_fee_") val standFee: Money? = null,
    val note: String? = null,
    val status: EventStatus = EventStatus.PLANNED,
    /** Set when the event is cancelled; cleared again if that is ever undone. */
    val cancelledOn: LocalDate? = null,
    /** Set when the event is closed (DONE); cleared when it's reopened. */
    val closedOn: LocalDate? = null,
    val createdAt: LocalDate = LocalDate.now()
) {
    /**
     * The day the event ends. Once it is before today, the event is listed
     * under "Past", whatever its status. Has no backing field, so Room ignores it.
     */
    val lastDay: LocalDate
        get() = endDate ?: eventDate

    /**
     * A closed event can be reopened until its last day is over. SQL checks
     * this too, plus that no product was assigned elsewhere meanwhile.
     */
    fun canReopen(today: LocalDate): Boolean =
        status == EventStatus.DONE && !lastDay.isBefore(today)
}