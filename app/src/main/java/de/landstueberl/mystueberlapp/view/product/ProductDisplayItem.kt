package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun ProductDisplayItem(
    product: Product,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = SageGreenDark,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                SageGreenLight.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val productDetails = product.details
            val productStatus = productDetails.status

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Selection Checkbox ──
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onTap() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SageGreenDark,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // ── Product Image ──
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SageGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = stringResource(R.string.products_screen_no_image),
                        tint = SageGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ── Product Details ──
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.details.description,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    product.details.purchasePrice?.let { price ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.products_screen_purchase_price),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                text = "${price.amount} ${price.currency.symbol}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    product.details.salesPrice?.let { price ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.products_screen_sales_price),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                text = "${price.amount} ${price.currency.symbol}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Status Icon ──
                when (productStatus) {
                    ProductStatus.REMOVED -> Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = stringResource(R.string.products_screen_status_removed),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    ProductStatus.SOLD -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.products_screen_status_sold),
                        tint = SageGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    ProductStatus.AVAILABLE -> Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = stringResource(R.string.products_screen_status_available),
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = SageGreen.copy(alpha = 0.2f)
            )

            // ── Bottom Date Row ────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SageGreen.copy(alpha = 0.07f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Created At ───────
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.products_screen_created_at,
                                product.details.createdAt.format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                )
                            ),
                            fontSize = 9.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ── Sold / Removed date ───────
                    val statusDate = when (productStatus) {
                        ProductStatus.SOLD -> productDetails.soldOn
                        ProductStatus.REMOVED -> productDetails.removedOn
                        ProductStatus.AVAILABLE -> null
                    }

                    if (statusDate != null) {
                        val statusColor = if (productStatus == ProductStatus.SOLD) {
                            SageGreen.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (productStatus == ProductStatus.SOLD) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Cancel
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    if (productStatus == ProductStatus.SOLD) {
                                        R.string.products_screen_sold_on
                                    } else {
                                        R.string.products_screen_removed_on
                                    },
                                    statusDate.format(DATE_FORMAT)
                                ),
                                fontSize = 9.sp,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}