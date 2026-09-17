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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.FloralWhite
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.viewmodel.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    val availableSalesAreaCount by viewModel.availableSalesAreaCount.collectAsStateWithLifecycle()
    val pendingOrderProductsCount by viewModel.pendingOrderProductsCount.collectAsStateWithLifecycle()
    val totalProductsCurrentYear by viewModel.totalProductsCurrentYear.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
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
                salesVolume = "€0,00",
                soldToday = 0,
                onSale = 0,
                onClick = { onNavigateToStatistics() }
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
                    onClick = { onNavigateToProducts() },
                    modifier = Modifier.weight(1f)
                )
                OrdersTile(
                    openOrders = 0,
                    nextOrder = null,
                    onClick = { onNavigateToOrders() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}