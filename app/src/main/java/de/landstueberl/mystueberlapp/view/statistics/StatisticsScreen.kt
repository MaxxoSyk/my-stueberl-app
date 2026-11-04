package de.landstueberl.mystueberlapp.view.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.rangeOf
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.viewmodel.statistics.StatisticsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onNavigateBack: () -> Unit
) {
    val period by viewModel.period.collectAsStateWithLifecycle()
    val periodFigures by viewModel.periodFigures.collectAsStateWithLifecycle()
    val overallFigures by viewModel.overallFigures.collectAsStateWithLifecycle()

    val currentYear = remember { LocalDate.now().year }
    val rangeCaption = remember(period) { period.rangeOf().asCaption() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.statistics_screen_title),
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (periodFigures == null || overallFigures == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }
            return@Scaffold
        }

        val figures = requireNotNull(periodFigures)
        val overall = requireNotNull(overallFigures)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Period Selector ────────────────
            StatisticsPeriodSelector(
                selected = period,
                onSelect = viewModel::setPeriod,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            // ── Period Figures ─────────────────
            StatisticsRow(
                label = stringResource(R.string.statistics_revenue_total),
                caption = rangeCaption,
                value = figures.revenueTotal.format(),
                icon = Icons.Default.AttachMoney,
                iconTint = AccentGold,
                valueColor = AccentGold,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_revenue_sales_area),
                caption = rangeCaption,
                value = figures.revenueSalesArea.format(),
                icon = Icons.Default.Storefront,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_revenue_orders),
                caption = rangeCaption,
                value = figures.revenueOrders.format(),
                icon = Icons.Default.Receipt,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_sold_count),
                caption = rangeCaption,
                value = figures.soldCount.toString(),
                icon = Icons.Default.ShoppingCart,
                iconTint = SageGreenDark,
                onClick = { }
            )

            // ── Overall ────────────────────────
            StatisticsSectionHeader(
                text = stringResource(R.string.statistics_section_overall)
            )

            StatisticsRow(
                label = stringResource(R.string.statistics_profit_year, currentYear),
                value = overall.profitThisYear.format(),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = AccentGold,
                valueColor = AccentGold,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_avg_sale_price_year, currentYear),
                value = overall.averageSalePriceThisYear?.format()
                    ?: stringResource(R.string.statistics_no_value),
                icon = Icons.Default.AttachMoney,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_avg_sale_price_all_time),
                value = overall.averageSalePriceAllTime?.format()
                    ?: stringResource(R.string.statistics_no_value),
                icon = Icons.Default.AttachMoney,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_avg_stock_presence),
                caption = stringResource(R.string.statistics_avg_stock_presence_caption),
                value = overall.averageStockPresenceDays
                    ?.let {
                        stringResource(
                            R.string.statistics_days_value,
                            Math.round(it).toInt()
                        )
                    }
                    ?: stringResource(R.string.statistics_no_value),
                icon = Icons.Default.Schedule,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_highest_sale_year, currentYear),
                value = overall.highestSalePriceThisYear.format(),
                icon = Icons.Default.EmojiEvents,
                iconTint = AccentGold,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_highest_profit_year, currentYear),
                value = overall.highestProfitThisYear.format(),
                icon = Icons.Default.EmojiEvents,
                iconTint = AccentGold,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_free_products_year, currentYear),
                value = overall.freeProductsThisYear.toString(),
                icon = Icons.Default.CardGiftcard,
                onClick = { }
            )
            StatisticsRow(
                label = stringResource(R.string.statistics_removed_products_year, currentYear),
                value = overall.removedProductsThisYear.toString(),
                icon = Icons.Default.Cancel,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { }
            )
        }
    }
}