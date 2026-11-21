package de.landstueberl.mystueberlapp.view.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.model.common.format
import de.landstueberl.mystueberlapp.data.model.till.SaleWithProducts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val DATE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

/** The event's completed sales, newest first. [onUndo] is null when undo isn't allowed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TillSalesSheet(
    sales: List<SaleWithProducts>,
    isWorking: Boolean,
    onUndo: ((saleId: Int) -> Unit)?,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.till_sales_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // Same as swiping down: closes the sheet and resets the search.
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = sales, key = { it.sale.id }) { entry ->
                    SaleRow(
                        entry = entry,
                        isWorking = isWorking,
                        onUndo = onUndo?.let { undo -> { undo(entry.sale.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleRow(
    entry: SaleWithProducts,
    isWorking: Boolean,
    onUndo: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    val sale = entry.sale

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = saleTimeText(sale.soldAt),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.pay_sheet_items,
                            sale.itemCount,
                            sale.itemCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = sale.total.format(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.tertiary
                )
            }

            if (entry.products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.products.joinToString(", ") { it.details.description },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onUndo != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onUndo,
                        enabled = !isWorking,
                        colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)
                    ) {
                        Text(stringResource(R.string.till_undo))
                    }
                }
            }
        }
    }
}

/** Time only for today's sales; with the date on later days of a multi-day event. */
@Composable
private fun saleTimeText(soldAt: Instant): String {
    val zone = remember { ZoneId.systemDefault() }
    val dateTime = soldAt.atZone(zone)
    return if (dateTime.toLocalDate() == LocalDate.now(zone)) {
        dateTime.format(TIME_FORMAT)
    } else {
        dateTime.format(DATE_TIME_FORMAT)
    }
}