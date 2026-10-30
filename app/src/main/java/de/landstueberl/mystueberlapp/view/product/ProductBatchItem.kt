package de.landstueberl.mystueberlapp.view.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import java.io.File

@Composable
fun ProductBatchItem(
    batch: ProductListItem.Batch,
    imageFileOf: (Product) -> File?,
    isExpanded: Boolean,
    selectedProducts: Set<Int>,
    isSelectionMode: Boolean,
    onToggleExpanded: () -> Unit,
    onProductTap: (Product) -> Unit,
    onProductLongPress: (Product) -> Unit,
    onImageTap: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val representative = batch.representative
    val details = representative.details
    val imageFile = imageFileOf(representative)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // ── Group Header ───────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Image ──
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SageGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageFile != null) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = stringResource(R.string.products_screen_no_image),
                            tint = SageGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ── Description + Count + Prices ──
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = details.description,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CountBadge(count = batch.count)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    details.salesPrice?.let { price ->
                        Text(
                            text = stringResource(
                                R.string.products_screen_batch_price_each,
                                price.format()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentGold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Expand Chevron ──
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = stringResource(
                        if (isExpanded) {
                            R.string.products_screen_batch_collapse
                        } else {
                            R.string.products_screen_batch_expand
                        }
                    ),
                    tint = SageGreenDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Members ────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                batch.products.forEach { product ->
                    ProductDisplayItem(
                        product = product,
                        imageFile = imageFileOf(product),
                        isSelected = selectedProducts.contains(product.details.id),
                        isSelectionMode = isSelectionMode,
                        onTap = { onProductTap(product) },
                        onLongPress = { onProductLongPress(product) },
                        onImageTap = { onImageTap(product) }
                    )
                }
            }
        }
    }
}

// ── Count Badge ────────────────────────────────────────────────────────
@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SageGreenDark.copy(alpha = 0.15f)
    ) {
        Text(
            text = stringResource(R.string.products_screen_batch_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = SageGreenDark,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}