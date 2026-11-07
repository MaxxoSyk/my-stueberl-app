package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.MaterialSuggestion
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.calculatedTotal
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary

/** No row is being edited. */
private const val NO_EDIT = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCalculatorDialog(
    positions: List<ProductMaterialDetail>,
    materialSuggestions: List<MaterialSuggestion>,
    onAdd: (ProductMaterialDetail) -> Unit,
    onUpdate: (index: Int, ProductMaterialDetail) -> Unit,
    onRemove: (index: Int) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Index of the row in edit mode, or NO_EDIT. A newly added row opens in
    // edit mode immediately, so it is always the last index.
    var editingIndex by remember { mutableIntStateOf(NO_EDIT) }

    val total = positions.calculatedTotal()

    fun addPosition() {
        onAdd(
            ProductMaterialDetail(
                productId = 0,
                name = "",
                quantity = 1,
                unitPrice = PriceInput.zeroMoney()
            )
        )
        editingIndex = positions.size   // the appended row
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_cancel),
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.cost_calculator_title_purchase),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    },
                    actions = {
                        if (positions.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    editingIndex = NO_EDIT
                                    onClear()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.cost_calculator_clear),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SageGreen,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                CalculatorBottomBar(
                    total = total,
                    isEditing = editingIndex != NO_EDIT,
                    onConfirm = onConfirm
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                if (positions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cost_calculator_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(positions) { index, position ->
                            MaterialPositionRow(
                                position = position,
                                materialSuggestions = materialSuggestions,
                                isEditing = index == editingIndex,
                                onStartEdit = { editingIndex = index },
                                onConfirmEdit = { updated ->
                                    onUpdate(index, updated)
                                    editingIndex = NO_EDIT
                                },
                                onCancelEdit = {
                                    // A row that was never filled in is discarded
                                    // rather than left blank in the list.
                                    if (position.name.isBlank()) {
                                        onRemove(index)
                                    }
                                    editingIndex = NO_EDIT
                                },
                                onRemove = {
                                    editingIndex = NO_EDIT
                                    onRemove(index)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = SageGreen.copy(alpha = 0.3f))

                // ── Add Action ─────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = { addPosition() },
                        enabled = editingIndex == NO_EDIT,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SageGreenDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.cost_calculator_add_position),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// ── Bottom Bar ─────────────────────────────────────────────────────────
@Composable
private fun CalculatorBottomBar(
    total: Money?,
    isEditing: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cost_calculator_total),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = total?.format() ?: "–",
                style = MaterialTheme.typography.titleLarge,
                color = AccentGold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onConfirm,
            enabled = !isEditing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SageGreen,
                contentColor = Color.White,
                disabledContainerColor = SageGreenLight,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = stringResource(R.string.cost_calculator_apply),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}