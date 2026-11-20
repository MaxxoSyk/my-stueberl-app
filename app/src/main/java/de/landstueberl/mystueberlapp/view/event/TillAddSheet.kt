package de.landstueberl.mystueberlapp.view.event

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import java.io.File

/**
 * Adds event products to the basket by hand. Stays open, so several items can
 * be added in a row. A batch row adds its next member; the basket bar at the
 * bottom confirms every add, since the till's snackbar is hidden behind the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TillAddSheet(
    items: List<ProductListItem>,
    query: String,
    basket: List<Product>,
    basketTotal: Money,
    imageFileOf: (Product) -> File?,
    onQueryChange: (String) -> Unit,
    onAdd: (productId: Int) -> Unit,
    onRemove: (productId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // How many members of each batch are in the basket, and the newest one
    // (the basket is oldest first, so associate keeps the last = newest).
    val inBasketByBatch = remember(basket) {
        basket.mapNotNull { it.details.batchId }.groupingBy { it }.eachCount()
    }
    val newestInBasketByBatch = remember(basket) {
        basket.filter { it.details.batchId != null }
            .associate { it.details.batchId!! to it.details.id }
    }

    // Vibrate only when an item actually landed in the basket.
    var lastBasketSize by remember { mutableIntStateOf(basket.size) }
    LaunchedEffect(basket.size) {
        if (basket.size > lastBasketSize) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        lastBasketSize = basket.size
    }

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
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.till_add_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.till_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_cancel))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = StueberlDefaults.textFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (query.isBlank()) R.string.till_add_empty else R.string.till_add_no_results
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = items, key = { it.key }) { item ->
                        val product = when (item) {
                            is ProductListItem.Single -> item.product
                            is ProductListItem.Batch -> item.representative
                        }
                        // Also for the last member left, which is shown as a single row.
                        val batchId = product.details.batchId
                        val inBasket = batchId?.let { inBasketByBatch[it] } ?: 0

                        TillAddRow(
                            product = product,
                            imageFile = imageFileOf(product),
                            count = (item as? ProductListItem.Batch)?.count,
                            inBasketCount = inBasket,
                            // Members are identical; take the next one.
                            onAdd = {
                                onAdd(
                                    when (item) {
                                        is ProductListItem.Single -> item.product.details.id
                                        is ProductListItem.Batch -> item.products.first().details.id
                                    }
                                )
                            },
                            onRemoveOne = batchId
                                ?.let { newestInBasketByBatch[it] }
                                ?.let { id -> { onRemove(id) } }
                        )
                    }
                }
            }

            BasketBar(itemCount = basket.size, total = basketTotal)
        }
    }
}

// ── Basket Bar ─────────────────────────────────────────────────────────
/** Always visible, so every add is confirmed even while the sheet covers the till. */
@Composable
private fun BasketBar(itemCount: Int, total: Money) {
    val colorScheme = MaterialTheme.colorScheme
    HorizontalDivider(color = colorScheme.primary.copy(alpha = 0.3f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(targetState = itemCount, label = "basketCount") { count ->
            Text(
                text = pluralStringResource(R.plurals.till_add_sheet_basket, count, count),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        AnimatedContent(targetState = total, label = "basketTotal") { value ->
            Text(
                text = value.format(),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.tertiary
            )
        }
    }
}

// ── Row ────────────────────────────────────────────────────────────────
@Composable
private fun TillAddRow(
    product: Product,
    imageFile: File?,
    inBasketCount: Int,
    onAdd: () -> Unit,
    onRemoveOne: (() -> Unit)?,
    count: Int? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    // Short highlight when one more of this product went into the basket.
    val flash = remember { Animatable(0f) }
    var previousCount by remember { mutableIntStateOf(inBasketCount) }
    LaunchedEffect(inBasketCount) {
        if (inBasketCount > previousCount) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 700))
        }
        previousCount = inBasketCount
    }

    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = lerp(colorScheme.surface, colorScheme.secondaryContainer, flash.value)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(imageFile = imageFile, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.details.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    count?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        BatchCountBadge(count = it)
                    }
                }
                Text(
                    text = product.details.salesPrice.format(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.tertiary
                )
                if (inBasketCount > 0) {
                    Text(
                        text = stringResource(R.string.till_add_sheet_in_basket, inBasketCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }
            }
            if (onRemoveOne != null) {
                IconButton(onClick = onRemoveOne) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = stringResource(R.string.till_add_sheet_remove_one),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = stringResource(R.string.till_add),
                tint = colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}