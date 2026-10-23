package de.landstueberl.mystueberlapp.view.order

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.data.labelRes
import de.landstueberl.mystueberlapp.data.total
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun OrderDisplayItem(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = order.orderDetail
    val productCount = order.productList.size
    val total = order.total

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Customer + Status ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = detail.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OrderStatusBadge(status = detail.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Due Date ───────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.orders_screen_due_date,
                        detail.dueDate.format(DATE_FORMAT)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // ── Note ───────────────────────────
            if (detail.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = SageGreen.copy(alpha = 0.2f)
            )

            // ── Products + Total ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.orders_screen_product_count,
                        productCount,
                        productCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.weight(1f))

                if (total != null) {
                    Text(
                        text = "${total.amount} ${total.currency.symbol}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGold
                    )
                }
            }

            // ── Created At ─────────────────────
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        R.string.orders_screen_created_at,
                        detail.createdAt.format(DATE_FORMAT)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Order Status Badge ─────────────────────────────────────────────────
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (status) {
        OrderStatus.OPEN -> Icons.Default.Schedule
        OrderStatus.RESOLVED -> Icons.Default.CheckCircle
        OrderStatus.CANCELLED -> Icons.Default.Cancel
    }

    val tint = when (status) {
        OrderStatus.OPEN -> AccentGold
        OrderStatus.RESOLVED -> SageGreen
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
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