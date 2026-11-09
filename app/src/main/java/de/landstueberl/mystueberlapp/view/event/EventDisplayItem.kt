package de.landstueberl.mystueberlapp.view.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.EventWithProductCount
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.labelRes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val EVENT_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun EventDisplayItem(
    item: EventWithProductCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = item.event
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Name + Status ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Festival,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                EventStatusBadge(status = event.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Date + Location ────────────────
            EventInfoRow(icon = Icons.Default.Schedule, text = eventDateText(event))

            event.location?.takeIf { it.isNotBlank() }?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                EventInfoRow(icon = Icons.Default.Place, text = location)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = colorScheme.primary.copy(alpha = 0.2f)
            )

            // ── Products ───────────────────────
            EventInfoRow(
                icon = Icons.Default.Inventory,
                text = pluralStringResource(
                    R.plurals.events_screen_product_count,
                    item.productCount,
                    item.productCount
                )
            )
        }
    }
}

@Composable
private fun EventInfoRow(icon: ImageVector, text: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** "12 Oct 2025", or "12 Oct 2025 – 14 Oct 2025" for a multi-day event. */
@Composable
fun eventDateText(event: EventDetail): String {
    val start = event.eventDate.format(EVENT_DATE_FORMAT)
    val end = event.endDate
    return if (end == null || end == event.eventDate) {
        start
    } else {
        stringResource(R.string.event_date_range, start, end.format(EVENT_DATE_FORMAT))
    }
}

// ── Event Status Badge ─────────────────────────────────────────────────
@Composable
fun EventStatusBadge(
    status: EventStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (icon, tint) = when (status) {
        EventStatus.PLANNED -> Icons.Default.Event to colorScheme.onSurfaceVariant
        EventStatus.IN_PREPARATION -> Icons.Default.Construction to colorScheme.tertiary
        EventStatus.IN_PROGRESS -> Icons.Default.Storefront to colorScheme.secondary
        EventStatus.DONE -> Icons.Default.CheckCircle to colorScheme.primary
        EventStatus.CANCELLED -> Icons.Default.Cancel to colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(status.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}