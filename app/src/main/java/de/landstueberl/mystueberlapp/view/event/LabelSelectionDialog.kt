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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.label.LabelSheetLayout
import de.landstueberl.mystueberlapp.data.model.common.format
import de.landstueberl.mystueberlapp.data.model.product.ProductListItem
import de.landstueberl.mystueberlapp.data.model.product.allProducts
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val PRINTED_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * Picks which of an event's available products get a QR label.
 * One label per product, so batch members can be ticked one by one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionDialog(
    items: List<ProductListItem>,
    selectedIds: Set<Int>,
    printedOn: Map<Int, LocalDate>,
    groupBatches: Set<Int>,
    selectedGroups: Set<Int>,
    batchLabelPrintedOn: Map<Int, LocalDate>,
    identifiers: Map<Int, String>,
    imageFileOf: (Product) -> File?,
    isPrinting: Boolean,
    onToggleProduct: (Int) -> Unit,
    onToggleBatch: (List<Int>) -> Unit,
    onSetGroupMode: (batchId: Int, group: Boolean) -> Unit,
    onToggleGroup: (batchId: Int) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    // Group mode only applies to batch rows (a last member alone is a single row).
    val activeGroups = remember(items, groupBatches) {
        groupBatches intersect items.filterIsInstance<ProductListItem.Batch>().mapTo(HashSet()) { it.batchId }
    }
    val productIds = remember(items, activeGroups) {
        items.allProducts().filter { it.details.batchId !in activeGroups }.map { it.details.id }
    }
    val count = productIds.count { it in selectedIds } + selectedGroups.count { it in activeGroups }
    val allSelected = count > 0 && count == productIds.size + activeGroups.size
    val pages = LabelSheetLayout.pageCount(count)

    // Mode switch waiting for the "remove old labels" warning: batchId to group mode.
    var pendingSwitch by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
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
                    val allPrinted = productIds.isNotEmpty() || activeGroups.isNotEmpty()
                    val everythingPrinted = allPrinted &&
                            productIds.all { it in printedOn } &&
                            activeGroups.all { it in batchLabelPrintedOn }
                    Text(
                        text = stringResource(
                            if (everythingPrinted) R.string.label_dialog_all_printed else R.string.label_dialog_hint
                        ),
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
                            onToggle = { onToggleProduct(item.product.details.id) },
                            printedOn = printedOn[item.product.details.id],
                            identifier = identifiers[item.product.details.id]
                        )

                        is ProductListItem.Batch -> LabelBatchGroup(
                            batch = item,
                            imageFileOf = imageFileOf,
                            selectedIds = selectedIds,
                            printedOn = printedOn,
                            identifiers = identifiers,
                            isGroup = item.batchId in activeGroups,
                            groupSelected = item.batchId in selectedGroups,
                            groupPrintedOn = batchLabelPrintedOn[item.batchId],
                            isExpanded = item.batchId in expandedBatchIds,
                            onToggleExpanded = {
                                if (!expandedBatchIds.add(item.batchId)) expandedBatchIds.remove(item.batchId)
                            },
                            onToggleBatch = { onToggleBatch(item.products.map { it.details.id }) },
                            onToggleProduct = onToggleProduct,
                            onToggleGroup = { onToggleGroup(item.batchId) },
                            onRequestMode = { toGroup ->
                                // Warn only if the other mode already has printed labels.
                                val needsWarning = if (toGroup) {
                                    item.products.any { it.details.id in printedOn }
                                } else {
                                    item.batchId in batchLabelPrintedOn
                                }
                                if (needsWarning) pendingSwitch = item.batchId to toGroup
                                else onSetGroupMode(item.batchId, toGroup)
                            }
                        )
                    }
                }
            }
        }

        pendingSwitch?.let { (batchId, toGroup) ->
            StueberlConfirmationDialog(
                title = stringResource(R.string.label_dialog_switch_title),
                message = stringResource(
                    if (toGroup) R.string.label_dialog_switch_to_group
                    else R.string.label_dialog_switch_to_products
                ),
                confirmLabel = stringResource(R.string.label_dialog_switch_confirm),
                onConfirm = {
                    pendingSwitch = null
                    onSetGroupMode(batchId, toGroup)
                },
                onDismiss = { pendingSwitch = null }
            )
        }
    }
}

// ── Rows ───────────────────────────────────────────────────────────────
@Composable
private fun LabelProductRow(
    product: Product,
    imageFile: File?,
    checked: Boolean,
    onToggle: () -> Unit,
    printedOn: LocalDate? = null,
    identifier: String? = null
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
            ProductThumbnailWithIdentifier(imageFile = imageFile, size = 40.dp, identifier = identifier)
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
                printedOn?.let { date ->
                    Text(
                        text = stringResource(R.string.label_dialog_printed_on, date.format(PRINTED_DATE_FORMAT)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }
            }
        }
    }
}

/** A batch: per product (tri-state, expandable) or one group label. */
@Composable
private fun LabelBatchGroup(
    batch: ProductListItem.Batch,
    imageFileOf: (Product) -> File?,
    selectedIds: Set<Int>,
    printedOn: Map<Int, LocalDate>,
    identifiers: Map<Int, String>,
    isGroup: Boolean,
    groupSelected: Boolean,
    groupPrintedOn: LocalDate?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleBatch: () -> Unit,
    onToggleProduct: (Int) -> Unit,
    onToggleGroup: () -> Unit,
    onRequestMode: (toGroup: Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val details = batch.representative.details
    val selectedCount = batch.products.count { it.details.id in selectedIds }
    val printedCount = batch.products.count { it.details.id in printedOn }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Card(
            // Only per-product mode has members to expand.
            onClick = if (isGroup) onToggleGroup else onToggleExpanded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGroup) {
                        Checkbox(
                            checked = groupSelected,
                            onCheckedChange = { onToggleGroup() },
                            colors = labelCheckboxColors()
                        )
                    } else {
                        TriStateCheckbox(
                            state = when (selectedCount) {
                                0 -> ToggleableState.Off
                                batch.count -> ToggleableState.On
                                else -> ToggleableState.Indeterminate
                            },
                            onClick = onToggleBatch,
                            colors = labelCheckboxColors()
                        )
                    }
                    ProductThumbnailWithIdentifier(
                        imageFile = imageFileOf(batch.representative),
                        size = 40.dp,
                        // Only the group's own identifier, once printed; never a member's.
                        identifier = if (isGroup && groupPrintedOn != null) {
                            identifiers[batch.representative.details.id]
                        } else {
                            null
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = details.description,
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BatchCountBadge(count = batch.count)
                        }
                        val status = when {
                            isGroup && groupPrintedOn != null -> stringResource(
                                R.string.label_dialog_group_printed_on,
                                groupPrintedOn.format(PRINTED_DATE_FORMAT)
                            )
                            isGroup -> pluralStringResource(
                                R.plurals.label_dialog_group_count, batch.count, batch.count
                            )
                            printedCount > 0 -> stringResource(
                                R.string.label_dialog_batch_printed, printedCount, batch.count
                            )
                            else -> stringResource(
                                R.string.label_dialog_batch_selected, selectedCount, batch.count
                            )
                        }
                        Text(status, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    if (!isGroup) {
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

                // Mode switch
                Row(
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isGroup,
                        onClick = { if (isGroup) onRequestMode(false) },
                        label = { Text(stringResource(R.string.label_dialog_mode_products)) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = isGroup,
                        onClick = { if (!isGroup) onRequestMode(true) },
                        label = { Text(stringResource(R.string.label_dialog_mode_group)) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded && !isGroup) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                batch.products.forEach { product ->
                    LabelProductRow(
                        product = product,
                        imageFile = imageFileOf(product),
                        checked = product.details.id in selectedIds,
                        onToggle = { onToggleProduct(product.details.id) },
                        printedOn = printedOn[product.details.id],
                        identifier = identifiers[product.details.id]
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