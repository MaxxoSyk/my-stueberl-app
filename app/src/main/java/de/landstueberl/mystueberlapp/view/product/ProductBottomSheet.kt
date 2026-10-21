package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val HINT_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * A product can only have been sold between the day it was added and today.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class SaleDateRange(private val createdAt: LocalDate) : SelectableDates {

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return !date.isBefore(createdAt) && !date.isAfter(LocalDate.now())
    }

    override fun isSelectableYear(year: Int): Boolean =
        year in createdAt.year..LocalDate.now().year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBottomSheet(
    product: Product,
    onMarkAsSold: (LocalDate) -> Unit,
    onMarkAsRemoved: () -> Unit,
    onResetToAvailable: () -> Unit,
    onGoToOrder: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showSoldDatePicker by remember { mutableStateOf(false) }
    val productStatus = product.details.status
    val details = product.details
    val isOrderProduct = details.orderId != null

    /** Animates the sheet away, then runs [action]. */
    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── Product Title ──────────────────
            Text(
                text = product.details.description,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = SageGreen.copy(alpha = 0.3f)
            )

            if (isOrderProduct) {
                // Status is controlled by the order, not here.
                Text(
                    text = stringResource(R.string.product_bottom_sheet_order_managed),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                BottomSheetItem(
                    icon = Icons.Default.Receipt,
                    iconTint = AccentGold,
                    label = stringResource(R.string.product_bottom_sheet_go_to_order),
                    onClick = onGoToOrder
                )
            } else {
                when (productStatus) {
                    ProductStatus.AVAILABLE -> {
                        BottomSheetItem(
                            icon = Icons.Default.CheckCircle,
                            iconTint = SageGreen,
                            label = stringResource(R.string.product_bottom_sheet_mark_sold),
                            onClick = { showSoldDatePicker = true }
                        )
                        BottomSheetItem(
                            icon = Icons.Default.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            label = stringResource(R.string.product_bottom_sheet_mark_removed),
                            onClick = { dismissThen(onMarkAsRemoved) }
                        )
                    }
                    ProductStatus.SOLD, ProductStatus.REMOVED -> {
                        BottomSheetItem(
                            icon = Icons.Default.Refresh,
                            iconTint = AccentGold,
                            label = stringResource(R.string.product_bottom_sheet_reset),
                            onClick = { dismissThen(onResetToAvailable) }
                        )
                    }
                }
            }

            // Edit is always available
            BottomSheetItem(
                icon = Icons.Default.Edit,
                iconTint = SageGreenDark,
                label = stringResource(R.string.product_bottom_sheet_edit),
                onClick = onEdit
            )
        }
    }

    if (showSoldDatePicker) {
        val today = LocalDate.now()
        val createdAt = details.createdAt

        // A product added today can only have been sold today.
        val initialDate = if (createdAt.isAfter(today)) createdAt else today

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            yearRange = createdAt.year..today.year,
            selectableDates = remember(createdAt) { SaleDateRange(createdAt) }
        )

        DatePickerDialog(
            onDismissRequest = { showSoldDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        val soldOn = datePickerState.selectedDateMillis
                            ?.let { millis ->
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            ?: today

                        showSoldDatePicker = false
                        dismissThen { onMarkAsSold(soldOn) }
                    }
                ) {
                    Text(stringResource(R.string.product_bottom_sheet_mark_sold_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoldDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            Column {
                Text(
                    text = stringResource(R.string.product_bottom_sheet_sold_date_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
                Text(
                    text = stringResource(
                        R.string.product_bottom_sheet_sold_date_hint,
                        createdAt.format(HINT_DATE_FORMAT)
                    ),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
                )
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ── Bottom Sheet Item ──────────────────────────────────────────────────
@Composable
private fun BottomSheetItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}