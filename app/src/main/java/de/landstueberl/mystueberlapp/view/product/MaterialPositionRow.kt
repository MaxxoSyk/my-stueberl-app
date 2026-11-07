package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.MaterialSuggestion
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.lineTotal
import de.landstueberl.mystueberlapp.data.materialKey
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.FloralWhite
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import java.math.BigDecimal

/**
 * One cost position, either read-only or in edit mode.
 */
@Composable
fun MaterialPositionRow(
    position: ProductMaterialDetail,
    isEditing: Boolean,
    materialSuggestions: List<MaterialSuggestion>,
    onStartEdit: () -> Unit,
    onConfirmEdit: (ProductMaterialDetail) -> Unit,
    onCancelEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isEditing) {
        MaterialPositionEditor(
            position = position,
            materialSuggestions = materialSuggestions,
            onConfirm = onConfirmEdit,
            onCancel = onCancelEdit,
            modifier = modifier
        )
    } else {
        MaterialPositionDisplay(
            position = position,
            onEdit = onStartEdit,
            onRemove = onRemove,
            modifier = modifier
        )
    }
}

// ── Read-only ──────────────────────────────────────────────────────────
@Composable
private fun MaterialPositionDisplay(
    position: ProductMaterialDetail,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = position.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.material_position_unit_line,
                        position.quantity,
                        position.unitPrice.format()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = Money(
                    amount = position.lineTotal,
                    currency = position.unitPrice.currency
                ).format(),
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold
            )

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.material_position_edit),
                    tint = SageGreenDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.material_position_remove),
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Edit mode ──────────────────────────────────────────────────────────
@Composable
private fun MaterialPositionEditor(
    position: ProductMaterialDetail,
    materialSuggestions: List<MaterialSuggestion>,
    onConfirm: (ProductMaterialDetail) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(position.name) }
    var quantity by remember { mutableStateOf(position.quantity.toString()) }
    var amount by remember {
        mutableStateOf(
            position.unitPrice.amount
                .takeIf { it.signum() != 0 }
                ?.toPlainString()
                ?: ""
        )
    }

    val parsedQuantity = quantity.toIntOrNull() ?: 0
    val parsedAmount = PriceInput.parseAmount(amount)

    val isValid = parsedAmount != null && name.isNotBlank() && parsedQuantity >= 1

    // Keyed by name, so the chips appear whenever the field matches a known
    // material — after tapping a suggestion, typing it in full, or when
    // editing an existing row.
    val suggestionsByKey = remember(materialSuggestions) {
        materialSuggestions.associateBy { materialKey(it.name) }
    }
    val priceSuggestions = suggestionsByKey[materialKey(name)]?.prices.orEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SageGreen.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.material_position_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                shape = RoundedCornerShape(10.dp),
                colors = materialFieldColors()
            )

            MaterialNameSuggestions(
                query = name,
                suggestions = materialSuggestions,
                onSelect = { name = it.name }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(QUANTITY_PATTERN)) {
                            quantity = input
                        }
                    },
                    label = { Text(stringResource(R.string.material_position_quantity)) },
                    modifier = Modifier.width(96.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = materialFieldColors()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (PriceInput.isValid(input)) {
                            amount = input
                        }
                    },
                    label = { Text(stringResource(R.string.material_position_unit_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    suffix = { Text("€", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    colors = materialFieldColors()
                )
            }

            PriceSuggestions(
                prices = priceSuggestions,
                currentAmount = parsedAmount,
                // Scale 2 → "2.50", which PriceInput.isValid accepts.
                onSelect = { price -> amount = price.amount.toPlainString() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        onConfirm(
                            position.copy(
                                name = name.trim(),
                                quantity = parsedQuantity,
                                unitPrice = Money(
                                    amount = requireNotNull(parsedAmount),
                                    currency = PriceInput.CURRENCY
                                )
                            )
                        )
                    },
                    enabled = isValid
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.material_position_confirm),
                        tint = if (isValid) SageGreenDark else TextSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun materialFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FloralWhite,
    unfocusedContainerColor = FloralWhite,
    focusedBorderColor = SageGreen,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
    focusedLabelColor = SageGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = SageGreen
)

private val QUANTITY_PATTERN = Regex("^\\d{0,3}$")

// ── Name suggestions ───────────────────────────────────────────────────
/**
 * Previously used material names matching what has been typed. Renders nothing
 * until [MIN_QUERY_LENGTH] characters are entered, and skips an exact match —
 * suggesting what is already in the field would be noise.
 */
@Composable
private fun MaterialNameSuggestions(
    query: String,
    suggestions: List<MaterialSuggestion>,
    onSelect: (MaterialSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val matches = remember(query, suggestions) {
        if (query.length < MIN_QUERY_LENGTH) {
            emptyList()
        } else {
            suggestions
                .filter {
                    it.name.contains(query, ignoreCase = true) &&
                            !it.name.equals(query.trim(), ignoreCase = true)
                }
                .take(MAX_SUGGESTIONS)
        }
    }

    if (matches.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        matches.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSelect(suggestion) },
                label = {
                    Text(
                        text = suggestion.name,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = SageGreenLight.copy(alpha = 0.4f),
                    labelColor = SageGreenDark
                ),
                border = null
            )
        }
    }
}

private const val MIN_QUERY_LENGTH = 3
private const val MAX_SUGGESTIONS = 8

// ── Price suggestions ──────────────────────────────────────────────────
/**
 * Previously paid unit prices for the material in the name field, most used
 * first. Renders nothing when there is no history. The chip matching the
 * entered price is shown as selected.
 */
@Composable
private fun PriceSuggestions(
    prices: List<Money>,
    currentAmount: BigDecimal?,
    onSelect: (Money) -> Unit,
    modifier: Modifier = Modifier
) {
    if (prices.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.material_position_price_suggestions),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            prices.forEach { price ->
                // Both sides are scale 2 (PriceInput / Money), so equals() is safe.
                val selected = currentAmount == price.amount

                FilterChip(
                    selected = selected,
                    onClick = { onSelect(price) },
                    label = {
                        Text(
                            text = price.format(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AccentGold.copy(alpha = 0.15f),
                        labelColor = SageGreenDark,
                        selectedContainerColor = SageGreen,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    border = null
                )
            }
        }
    }
}