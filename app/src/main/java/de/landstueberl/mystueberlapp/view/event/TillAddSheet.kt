package de.landstueberl.mystueberlapp.view.event

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import java.io.File

/**
 * Adds event products to the basket by hand. Stays open, so several items can
 * be added in a row. A batch row adds its next member.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TillAddSheet(
    items: List<ProductListItem>,
    query: String,
    imageFileOf: (Product) -> File?,
    onQueryChange: (String) -> Unit,
    onAdd: (productId: Int) -> Unit,
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
                .imePadding()
        ) {
            Text(
                text = stringResource(R.string.till_add_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.till_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.common_cancel)
                            )
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
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = items, key = { it.key }) { item ->
                        when (item) {
                            is ProductListItem.Single -> TillAddRow(
                                product = item.product,
                                imageFile = imageFileOf(item.product),
                                onAdd = { onAdd(item.product.details.id) }
                            )

                            is ProductListItem.Batch -> TillAddRow(
                                product = item.representative,
                                imageFile = imageFileOf(item.representative),
                                count = item.count,
                                // Members are identical; take the next one.
                                onAdd = { onAdd(item.products.first().details.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TillAddRow(
    product: Product,
    imageFile: File?,
    onAdd: () -> Unit,
    count: Int? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
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