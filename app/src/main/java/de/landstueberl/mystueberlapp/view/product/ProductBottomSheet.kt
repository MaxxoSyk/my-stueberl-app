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
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.view.common.StueberlDatePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val HINT_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBottomSheet(
    product: Product,
    onMarkAsSold: (LocalDate) -> Unit,
    onMarkAsRemoved: (LocalDate) -> Unit,
    onResetToAvailable: () -> Unit,
    onGoToOrder: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    onGoToEvent: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showSoldDatePicker by remember { mutableStateOf(false) }
    var showRemovedDatePicker by remember { mutableStateOf(false) }
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
                text = details.description,
                style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.bodyMedium,
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
                // Event products keep their status actions (rule 1a); the
                // event only adds a shortcut to it.
                if (onGoToEvent != null) {
                    BottomSheetItem(
                        icon = Icons.Default.Festival,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = stringResource(R.string.product_bottom_sheet_go_to_event),
                        onClick = onGoToEvent
                    )
                }

                when (details.status) {
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
                            onClick = { showRemovedDatePicker = true }
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
        ProductStatusDateDialog(
            createdAt = details.createdAt,
            title = stringResource(R.string.product_bottom_sheet_sold_date_title),
            confirmLabel = stringResource(R.string.product_bottom_sheet_mark_sold_confirm),
            onConfirm = { soldOn ->
                showSoldDatePicker = false
                dismissThen { onMarkAsSold(soldOn) }
            },
            onDismiss = { showSoldDatePicker = false }
        )
    }

    if (showRemovedDatePicker) {
        ProductStatusDateDialog(
            createdAt = details.createdAt,
            title = stringResource(R.string.product_bottom_sheet_removed_date_title),
            confirmLabel = stringResource(R.string.product_bottom_sheet_mark_removed_confirm),
            isDestructive = true,
            onConfirm = { removedOn ->
                showRemovedDatePicker = false
                dismissThen { onMarkAsRemoved(removedOn) }
            },
            onDismiss = { showRemovedDatePicker = false }
        )
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Status Date Picker ─────────────────────────────────────────────────
/**
 * Asks for the date on which a product changed status. Confirming performs the
 * action, so the common "it happened today" case is a single extra tap.
 *
 * Selectable range is [createdAt]…today: a product's status cannot have changed
 * before it existed, nor in the future.
 */
@Composable
private fun ProductStatusDateDialog(
    createdAt: LocalDate,
    title: String,
    confirmLabel: String,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    val today = remember { LocalDate.now() }

    StueberlDatePickerDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        initialDate = today,
        minDate = createdAt,
        maxDate = today,
        title = title,
        hint = stringResource(
            R.string.product_bottom_sheet_date_hint,
            createdAt.format(HINT_DATE_FORMAT)
        ),
        confirmLabel = confirmLabel,
        isDestructive = isDestructive
    )
}