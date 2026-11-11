package de.landstueberl.mystueberlapp.view.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.view.product.ProductBatchItem
import de.landstueberl.mystueberlapp.view.product.ProductDisplayItem
import java.io.File

/**
 * Picks sales-area products to attach to an order or event.
 *
 * Reuses the products list rows, which already render a checkbox in selection
 * mode — so `isSelectionMode` is always true here.
 *
 * @param emptyText shown when nothing can be attached; defaults to the order text
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachProductDialog(
    items: List<ProductListItem>,
    selectedIds: Set<Int>,
    imageFileOf: (Product) -> File?,
    onToggleProduct: (Int) -> Unit,
    onToggleBatch: (List<Int>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    emptyText: String = stringResource(R.string.attach_product_empty)
) {
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
                                contentDescription = stringResource(R.string.common_cancel),
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.attach_product_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SageGreen,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = Color.White,
                            disabledContainerColor = SageGreenLight,
                            disabledContentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = if (selectedIds.isEmpty()) {
                                stringResource(R.string.attach_product_confirm_none)
                            } else {
                                pluralStringResource(
                                    R.plurals.attach_product_confirm,
                                    selectedIds.size,
                                    selectedIds.size
                                )
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = items,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is ProductListItem.Single -> {
                                val product = item.product
                                ProductDisplayItem(
                                    product = product,
                                    imageFile = remember(product) { imageFileOf(product) },
                                    isSelected = selectedIds.contains(product.details.id),
                                    isSelectionMode = true,
                                    onTap = { onToggleProduct(product.details.id) },
                                    onLongPress = { onToggleProduct(product.details.id) },
                                    onImageTap = { }
                                )
                            }

                            is ProductListItem.Batch -> {
                                val memberIds = item.products.map { it.details.id }

                                ProductBatchItem(
                                    batch = item,
                                    imageFileOf = imageFileOf,
                                    isExpanded = expandedBatchIds.contains(item.batchId),
                                    selectedProducts = selectedIds,
                                    isSelectionMode = true,
                                    onToggleExpanded = {
                                        if (!expandedBatchIds.add(item.batchId)) {
                                            expandedBatchIds.remove(item.batchId)
                                        }
                                    },
                                    onProductTap = { onToggleProduct(it.details.id) },
                                    onProductLongPress = { onToggleProduct(it.details.id) },
                                    onImageTap = { },
                                    onHeaderLongPress = {
                                        // Expand so the selection is visible — five
                                        // ticked checkboxes behind a collapsed header
                                        // look like nothing happened.
                                        expandedBatchIds.add(item.batchId)
                                        onToggleBatch(memberIds)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}