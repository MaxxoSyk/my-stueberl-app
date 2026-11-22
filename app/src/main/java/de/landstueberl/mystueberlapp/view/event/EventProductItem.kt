package de.landstueberl.mystueberlapp.view.event

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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.model.common.format
import de.landstueberl.mystueberlapp.data.model.product.ProductListItem
import de.landstueberl.mystueberlapp.data.model.product.ProductStatus
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val STATUS_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Products that are no longer available are faded, but stay readable. */
private const val NO_LONGER_AVAILABLE_ALPHA = 0.6f

// ── Single Product ─────────────────────────────────────────────────────
/**
 * One product of an event. Tapping opens it; [onRemove] adds a remove icon.
 * With [showStatus], the row is faded and shows when it was sold or removed.
 */
@Composable
fun EventProductItem(
    product: Product,
    imageFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    showStatus: Boolean = false,
    identifier: String? = null,
    onImageClick: (() -> Unit)? = null
) {
    val details = product.details
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (showStatus) NO_LONGER_AVAILABLE_ALPHA else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductThumbnailWithIdentifier(
                    imageFile = imageFile,
                    size = 56.dp,
                    identifier = identifier,
                    onClick = onImageClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Same layout as the products list: purchase first, then sales.
                    PriceRow(
                        label = stringResource(R.string.products_screen_purchase_price),
                        value = details.purchasePrice.format(),
                        valueStyle = MaterialTheme.typography.bodySmall,
                        valueColor = colorScheme.onSurfaceVariant,
                        isCalculated = product.materialList.isNotEmpty()
                    )
                    PriceRow(
                        label = stringResource(R.string.products_screen_sales_price),
                        value = details.salesPrice.format(),
                        valueStyle = MaterialTheme.typography.labelMedium,
                        valueColor = colorScheme.tertiary
                    )

                    if (showStatus) {
                        statusText(product)?.let { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (details.status == ProductStatus.REMOVED) {
                                    colorScheme.error
                                } else {
                                    colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.event_detail_remove_product),
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** "Sold: 12 Oct 2025" / "Removed: …", or just the status if no date was stored. */
@Composable
private fun statusText(product: Product): String? {
    val details = product.details
    return when (details.status) {
        ProductStatus.SOLD -> details.soldOn
            ?.let { stringResource(R.string.products_screen_sold_on, it.format(STATUS_DATE_FORMAT)) }
            ?: stringResource(R.string.products_screen_status_sold)
        ProductStatus.REMOVED -> details.removedOn
            ?.let { stringResource(R.string.products_screen_removed_on, it.format(STATUS_DATE_FORMAT)) }
            ?: stringResource(R.string.products_screen_status_removed)
        ProductStatus.AVAILABLE -> null
    }
}

// ── Batch ──────────────────────────────────────────────────────────────
/**
 * A batch of identical products. The header toggles the member list;
 * [onRemoveBatch] removes all members, [onRemoveProduct] single ones.
 */
@Composable
fun EventProductBatchItem(
    batch: ProductListItem.Batch,
    imageFileOf: (Product) -> File?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveBatch: (() -> Unit)? = null,
    onRemoveProduct: ((Product) -> Unit)? = null,
    /** Shown on the header when all members share one, e.g. a group label's. */
    headerIdentifier: String? = null,
    identifierOf: (Product) -> String? = { null },
    onImageClick: ((Product) -> Unit)? = null
) {
    val details = batch.representative.details
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductThumbnailWithIdentifier(
                    imageFile = imageFileOf(batch.representative),
                    size = 56.dp,
                    identifier = headerIdentifier,
                    onClick = onImageClick?.let { open -> { open(batch.representative) } }
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.products_screen_batch_price_each,
                            details.salesPrice.format()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.tertiary
                    )
                }

                if (onRemoveBatch != null) {
                    IconButton(onClick = onRemoveBatch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.event_detail_remove_batch),
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) {
                            R.string.products_screen_batch_collapse
                        } else {
                            R.string.products_screen_batch_expand
                        }
                    ),
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                batch.products.forEach { product ->
                    EventProductItem(
                        product = product,
                        imageFile = imageFileOf(product),
                        onClick = { onProductClick(product) },
                        onRemove = onRemoveProduct?.let { remove -> { remove(product) } },
                        identifier = identifierOf(product),
                        onImageClick = onImageClick?.let { open -> { open(product) } }
                    )
                }
            }
        }
    }
}

// ── Building Blocks ────────────────────────────────────────────────────
/** Label + price, aligned like the products list rows. */
@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueStyle: TextStyle,
    valueColor: Color,
    isCalculated: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = valueStyle,
            color = valueColor
        )
        if (isCalculated) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = stringResource(R.string.products_screen_price_calculated),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
internal fun ProductThumbnail(
    imageFile: File?,
    size: Dp,
    /** Opens the image fullscreen; only used when there is an image. */
    onClick: (() -> Unit)? = null
) {
    // Only clickable when there's an image to show.
    val openImage = onClick?.takeIf { imageFile != null }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (openImage != null) Modifier.clickable(onClick = openImage) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageFile != null) {
            AsyncImage(
                model = imageFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (openImage != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = stringResource(R.string.products_screen_no_image),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Thumbnail with the label identifier centered below it, if there is one. */
@Composable
internal fun ProductThumbnailWithIdentifier(
    imageFile: File?,
    size: Dp,
    identifier: String?,
    onClick: (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProductThumbnail(imageFile = imageFile, size = size, onClick = onClick)
        identifier?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun BatchCountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    ) {
        Text(
            text = stringResource(R.string.products_screen_batch_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}