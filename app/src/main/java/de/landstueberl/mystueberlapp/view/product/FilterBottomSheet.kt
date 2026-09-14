package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filter: ProductFilter,
    availableYears: List<Int>,
    onYearToggle: (Int) -> Unit,
    onStatusToggle: (ProductStatus) -> Unit,
    onProductSourceFilterChange: (ProductSourceFilter) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title ──────────────────────────
            Text(
                text = stringResource(R.string.products_filter_sheet_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            HorizontalDivider(color = SageGreen.copy(alpha = 0.3f))

            // ── Year Filter ────────────────────
            FilterSection(
                title = stringResource(R.string.products_filter_sheet_year)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableYears.forEach { year ->
                        FilterChip(
                            selected = filter.years.contains(year),
                            onClick = { onYearToggle(year) },
                            label = { Text(year.toString()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = SageGreen.copy(alpha = 0.3f))

            // ── Status Filter ──────────────────
            FilterSection(
                title = stringResource(R.string.products_filter_sheet_status)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter.statuses.contains(ProductStatus.AVAILABLE),
                        onClick = { onStatusToggle(ProductStatus.AVAILABLE) },
                        label = { Text(stringResource(R.string.products_filter_sheet_status_available)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filter.statuses.contains(ProductStatus.SOLD),
                        onClick = { onStatusToggle(ProductStatus.SOLD) },
                        label = { Text(stringResource(R.string.products_filter_sheet_status_sold)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filter.statuses.contains(ProductStatus.REMOVED),
                        onClick = { onStatusToggle(ProductStatus.REMOVED) },
                        label = { Text(stringResource(R.string.products_filter_sheet_status_removed)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            HorizontalDivider(color = SageGreen.copy(alpha = 0.3f))

            // ── Order Filter ───────────────────
            FilterSection(
                title = stringResource(R.string.products_filter_sheet_source)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter.productSourceFilter == ProductSourceFilter.ALL,
                        onClick = { onProductSourceFilterChange(ProductSourceFilter.ALL) },
                        label = { Text(stringResource(R.string.products_filter_sheet_source_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filter.productSourceFilter == ProductSourceFilter.FROM_ORDER,
                        onClick = { onProductSourceFilterChange(ProductSourceFilter.FROM_ORDER) },
                        label = { Text(stringResource(R.string.products_filter_sheet_source_ordered)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filter.productSourceFilter == ProductSourceFilter.FROM_SALES_AREA,
                        onClick = { onProductSourceFilterChange(ProductSourceFilter.FROM_SALES_AREA) },
                        label = { Text(stringResource(R.string.products_filter_sheet_source_sales_area)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            HorizontalDivider(color = SageGreen.copy(alpha = 0.3f))

            // ── Buttons ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Reset Button ──
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SageGreenDark
                    )
                ) {
                    Text(stringResource(R.string.products_filter_sheet_reset))
                }

                // ── Apply Button ──
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.products_filter_sheet_apply))
                }
            }
        }
    }
}

// ── Filter Section ─────────────────────────────────────────────────────
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = TextSecondary
        )
        content()
    }
}