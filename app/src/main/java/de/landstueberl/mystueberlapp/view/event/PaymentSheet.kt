package de.landstueberl.mystueberlapp.view.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import java.math.BigDecimal

/** Banknotes offered as quick buttons; only those above the total are shown. */
private val QUICK_NOTES: List<BigDecimal> = listOf(5, 10, 20, 50).map { BigDecimal(it).setScale(2) }

/**
 * Cash payment for the basket. Closable at any time, e.g. when the customer
 * grabs one more item. The cash given isn't stored; it's only for the change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    total: Money,
    itemCount: Int,
    isWorking: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var cashInput by rememberSaveable { mutableStateOf("") }

    // Both scale 2 (PriceInput / Money), so differences are scale 2 too.
    val cash = PriceInput.parseAmount(cashInput)
    val change = cash?.takeIf { it >= total.amount }?.subtract(total.amount)
    val missing = cash?.takeIf { it < total.amount }?.let { total.amount.subtract(it) }
    val notes = remember(total) { QUICK_NOTES.filter { it > total.amount } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Total ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pay_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(R.plurals.pay_sheet_items, itemCount, itemCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = total.format(),
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.tertiary
                )
            }

            // ── Cash given ──
            OutlinedTextField(
                value = cashInput,
                onValueChange = { if (PriceInput.isValid(it)) cashInput = it },
                label = { Text(stringResource(R.string.pay_cash_given)) },
                suffix = { Text(PriceInput.CURRENCY.symbol, color = colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = StueberlDefaults.textFieldColors()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCashChip(
                    label = stringResource(R.string.pay_exact),
                    selected = cash?.compareTo(total.amount) == 0,
                    onClick = { cashInput = PriceInput.toInputText(total.amount) }
                )
                notes.forEach { note ->
                    QuickCashChip(
                        label = "${note.toInt()} ${PriceInput.CURRENCY.symbol}",
                        selected = cash?.compareTo(note) == 0,
                        onClick = { cashInput = PriceInput.toInputText(note) }
                    )
                }
            }

            // ── Change ──
            when {
                change != null -> ChangePanel(
                    label = stringResource(R.string.pay_change),
                    value = Money(change, PriceInput.CURRENCY).format()
                )
                missing != null -> Text(
                    text = stringResource(
                        R.string.pay_missing,
                        Money(missing, PriceInput.CURRENCY).format()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.error
                )
            }

            // ── Actions ──
            Button(
                onClick = onComplete,
                enabled = change != null && !isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = StueberlDefaults.primaryButtonColors()
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.pay_complete),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.pay_back),
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickCashChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorScheme.primary,
            selectedLabelColor = colorScheme.onPrimary
        )
    )
}

@Composable
private fun ChangePanel(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.secondary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = colorScheme.secondary
            )
        }
    }
}