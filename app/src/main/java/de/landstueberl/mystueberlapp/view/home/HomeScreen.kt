package de.landstueberl.mystueberlapp.view.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.FloralWhite
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel
import java.time.LocalDate

/** Shown while the statistics flow has yet to emit. */
private const val PLACEHOLDER = "–"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val availableSalesAreaCount by viewModel.availableSalesAreaCount.collectAsStateWithLifecycle()
    val pendingOrderProductsCount by viewModel.pendingOrderProductsCount.collectAsStateWithLifecycle()
    val totalProductsCurrentYear by viewModel.totalProductsCurrentYear.collectAsStateWithLifecycle()
    val openOrderCount by viewModel.openOrderCount.collectAsStateWithLifecycle()
    val nextDueOrder by viewModel.nextDueOrder.collectAsStateWithLifecycle()
    val totalOrdersCurrentYear by viewModel.totalOrdersCurrentYear.collectAsStateWithLifecycle()
    val statisticsFigures by viewModel.statisticsTileFigures.collectAsStateWithLifecycle()

    val currentYear = remember { LocalDate.now().year }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_screen_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen,
                    titleContentColor = FloralWhite
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Statistics Tile - Full Width ──
            StatisticsTile(
                revenueToday = statisticsFigures?.revenueToday?.format() ?: PLACEHOLDER,
                soldToday = statisticsFigures?.soldToday ?: 0,
                revenueThisYear = statisticsFigures?.revenueThisYear?.format() ?: PLACEHOLDER,
                year = currentYear,
                onClick = onNavigateToStatistics
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Products & Orders Tiles - Side by Side ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProductsTile(
                    availableSalesAreaProducts = availableSalesAreaCount,
                    pendingOrderProducts = pendingOrderProductsCount,
                    totalProductsCurrentYear = totalProductsCurrentYear,
                    year = currentYear,
                    onClick = { onNavigateToProducts() },
                    modifier = Modifier.weight(1f)
                )
                OrdersTile(
                    openOrders = openOrderCount,
                    nextDueDate = nextDueOrder?.dueDate,
                    totalOrdersCurrentYear = totalOrdersCurrentYear,
                    year = currentYear,
                    onClick = { onNavigateToOrders() },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Gallery Tile - Full Width ──
            GalleryTile(
                onClick = onNavigateToGallery
            )
        }
    }
}