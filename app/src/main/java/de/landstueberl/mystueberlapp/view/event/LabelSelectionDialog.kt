package de.landstueberl.mystueberlapp.view.event

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.allProducts
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.label.LabelSheetLayout
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import java.io.File

/**
 * Picks which of an event's available products get a QR label.
 * One label per product, so batch members can be ticked one by one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionDialog(
    items: List<ProductListItem>,
    selectedIds: Set<Int>,
    imageFileOf: (Product) -> File?,
    isPrinting: Boolean,
    onToggleProduct: (Int) -> Unit,
    onToggleBatch: (List<Int>) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val allIds = remember(items) { items.allProducts().map { it.details.id } }
    // Only ticks still in the list count; a product may have been sold meanwhile.
    val count = allIds.count { it in selectedIds }
    val allSelected = allIds.isNotEmpty() && count == allIds.size
    val pages = LabelSheetLayout.pageCount(count)
    val expandedBatchIds = remember { mutableStateSetOf<Int>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_cancel)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.label_dialog_title),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = if (allSelected) onSelectNone else onSelectAll,
                            colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.onPrimary)
                        ) {
                            Text(
                                stringResource(
                                    if (allSelected) R.string.label_dialog_select_none
                                    else R.string.label_dialog_select_all
                                )
                            )
                        }
                    },
                    colors = StueberlDefaults.topAppBarColors()
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (count > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.label_dialog_pages, pages, pages),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = onPrint,
                        enabled = count > 0 && !isPrinting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = StueberlDefaults.primaryButtonColors()
                    ) {
                        when {
                            isPrinting -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            count == 0 -> Text(
                                text = stringResource(R.string.attach_product_confirm_none),
                                style = MaterialTheme.typography.labelLarge
                            )
                            else -> {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pluralStringResource(R.plurals.label_dialog_confirm, count, count),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            },
            containerColor = colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.label_dialog_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                items(items = items, key = { it.key }) { item ->
                    when (item) {
                        is ProductListItem.Single -> LabelProductRow(
                            product = item.product,
                            imageFile = imageFileOf(item.product),
                            checked = item.product.details.id in selectedIds,
                            onToggle = { onToggleProduct(item.product.details.id) }
                        )

                        is ProductListItem.Batch -> LabelBatchGroup(
                            batch = item,
                            imageFileOf = imageFileOf,
                            selectedIds = selectedIds,
                            isExpanded = item.batchId in expandedBatchIds,
                            onToggleExpanded = {
                                if (!expandedBatchIds.add(item.batchId)) expandedBatchIds.remove(item.batchId)
                            },
                            onToggleBatch = { onToggleBatch(item.products.map { it.details.id }) },
                            onToggleProduct = onToggleProduct
                        )
                    }
                }
            }
        }
    }
}

// ── Rows ───────────────────────────────────────────────────────────────
@Composable
private fun LabelProductRow(
    product: Product,
    imageFile: File?,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() }, colors = labelCheckboxColors())
            ProductThumbnail(imageFile = imageFile, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.details.description,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.details.salesPrice.format(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.tertiary
                )
            }
        }
    }
}

/** Header ticks all / none; expanding allows single members. */
@Composable
private fun LabelBatchGroup(
    batch: ProductListItem.Batch,
    imageFileOf: (Product) -> File?,
    selectedIds: Set<Int>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleBatch: () -> Unit,
    onToggleProduct: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedCount = batch.products.count { it.details.id in selectedIds }
    val state = when (selectedCount) {
        0 -> ToggleableState.Off
        batch.count -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Card(
            onClick = onToggleExpanded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateCheckbox(state = state, onClick = onToggleBatch, colors = labelCheckboxColors())
                ProductThumbnail(imageFile = imageFileOf(batch.representative), size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = batch.representative.details.description,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BatchCountBadge(count = batch.count)
                    }
                    Text(
                        text = stringResource(R.string.label_dialog_batch_selected, selectedCount, batch.count),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.products_screen_batch_collapse
                        else R.string.products_screen_batch_expand
                    ),
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                batch.products.forEach { product ->
                    LabelProductRow(
                        product = product,
                        imageFile = imageFileOf(product),
                        checked = product.details.id in selectedIds,
                        onToggle = { onToggleProduct(product.details.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun labelCheckboxColors(): CheckboxColors = with(MaterialTheme.colorScheme) {
    CheckboxDefaults.colors(
        checkedColor = secondary,
        uncheckedColor = onSurfaceVariant,
        checkmarkColor = onSecondary
    )
}