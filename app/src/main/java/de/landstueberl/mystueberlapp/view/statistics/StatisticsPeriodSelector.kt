package de.landstueberl.mystueberlapp.view.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.data.StatisticsPeriod
import de.landstueberl.mystueberlapp.ui.theme.SageGreen

// ── Period Selector ────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsPeriodSelector(
    selected: StatisticsPeriod,
    onSelect: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatisticsPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        text = stringResource(period.labelRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SageGreen,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}