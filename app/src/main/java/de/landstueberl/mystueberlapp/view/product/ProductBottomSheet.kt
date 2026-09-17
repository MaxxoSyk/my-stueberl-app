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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBottomSheet(
    product: Product,
    onMarkAsSold: () -> Unit,
    onMarkAsRemoved: () -> Unit,
    onResetToAvailable: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val productStatus = product.details.status

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

            when (productStatus) {
                ProductStatus.AVAILABLE -> {
                    BottomSheetItem(
                        icon = Icons.Default.CheckCircle,
                        iconTint = SageGreen,
                        label = stringResource(R.string.product_bottom_sheet_mark_sold),
                        onClick = { dismissThen(onMarkAsSold) }
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

            // ── Edit Product ───────────────────
            BottomSheetItem(
                icon = Icons.Default.Edit,
                iconTint = SageGreenDark,
                label = stringResource(R.string.product_bottom_sheet_edit),
                onClick = { dismissThen(onEdit) }
            )
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