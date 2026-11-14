package de.landstueberl.mystueberlapp.view.event

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.BasketAddResult
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.TillFigures
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.label.ProductScanner
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import de.landstueberl.mystueberlapp.viewmodel.event.EventTillViewModel
import de.landstueberl.mystueberlapp.viewmodel.event.TillMessage
import kotlinx.coroutines.launch
import java.io.File

/** FAB (56 dp) plus its margin and some air, so the last basket item stays tappable. */
private val FAB_CLEARANCE = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTillScreen(
    viewModel: EventTillViewModel,
    onNavigateBack: () -> Unit
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val basket by viewModel.basket.collectAsStateWithLifecycle()
    val basketTotal by viewModel.basketTotal.collectAsStateWithLifecycle()
    val figures by viewModel.figures.collectAsStateWithLifecycle()
    val addableItems by viewModel.addableItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showPaySheet by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scanner = remember(context) { ProductScanner.client(context) }
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ProductScanner.preload(context.applicationContext)
    }

    LaunchedEffect(viewModel) {
        // Named, so the launch below clearly uses the effect's scope: snackbars
        // are cancelled with it when the till screen leaves.
        val effectScope = this
        viewModel.messages.collect { message ->
            haptic.performHapticFeedback(
                if (message.isSuccess) HapticFeedbackType.Confirm else HapticFeedbackType.Reject
            )
            val text = tillMessageText(context, message) ?: return@collect
            // Newest message first, instead of queueing behind the previous one.
            snackbarHostState.currentSnackbarData?.dismiss()
            effectScope.launch { snackbarHostState.showSnackbar(text) }
        }
    }

    // Close both sheets if the event stops running or the basket empties, so
    // they don't pop up again by themselves later.
    LaunchedEffect(isRunning, basket.isEmpty()) {
        if (!isRunning) showAddSheet = false
        if (!isRunning || basket.isEmpty()) showPaySheet = false
    }

    val imageFileOf: (Product) -> File? = remember(viewModel) {
        { product -> product.imageList.firstOrNull()?.fileName?.let(viewModel::imageFile) }
    }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.till_title),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        event?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (basket.isNotEmpty() && isRunning) {
                        IconButton(
                            onClick = { showClearConfirmation = true },
                            enabled = !isWorking
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.till_clear_basket)
                            )
                        }
                    }
                },
                colors = StueberlDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            if (event != null) {
                EventBottomBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.till_total),
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = basketTotal.format(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.tertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddSheet = true },
                            enabled = isRunning,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.till_add),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = { showPaySheet = true },
                            enabled = isRunning && basket.isNotEmpty() && !isWorking,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = StueberlDefaults.primaryButtonColors()
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.till_pay),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (event != null && isRunning) {
                FloatingActionButton(
                    onClick = {
                        // One scanner at a time, even on a double tap.
                        if (!isScanning) {
                            isScanning = true
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    viewModel.onScanned(barcode.rawValue.orEmpty())
                                }
                                .addOnFailureListener(viewModel::onScannerFailed)
                                // Also runs when the user backs out of the scanner.
                                .addOnCompleteListener { isScanning = false }
                        }
                    },
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.till_scan)
                    )
                }
            }
        },
        containerColor = colorScheme.background
    ) { padding ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FAB_CLEARANCE),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isRunning) {
                    item(key = "not_running") { HintText(stringResource(R.string.till_not_running)) }
                }

                item(key = "figures") { TillFiguresCard(figures = figures) }

                item(key = "basket_header") {
                    DetailSectionHeader(
                        text = stringResource(R.string.till_basket, basket.size),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (basket.isEmpty()) {
                    item(key = "basket_empty") { HintText(stringResource(R.string.till_basket_empty)) }
                } else {
                    items(items = basket, key = { it.details.id }) { product ->
                        BasketItemRow(
                            product = product,
                            imageFile = imageFileOf(product),
                            onRemove = if (isRunning) {
                                { viewModel.removeFromBasket(product.details.id) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    // Closed by the LaunchedEffect above when the event stops running.
    if (showAddSheet) {
        TillAddSheet(
            items = addableItems,
            query = searchQuery,
            imageFileOf = imageFileOf,
            onQueryChange = viewModel::onSearchQueryChange,
            onAdd = viewModel::addToBasket,
            onDismiss = {
                showAddSheet = false
                viewModel.onSearchQueryChange("")
            }
        )
    }

    if (showPaySheet) {
        PaymentSheet(
            total = basketTotal,
            itemCount = basket.size,
            isWorking = isWorking,
            onComplete = {
                // Exactly what the customer saw; the repository refuses any difference.
                viewModel.completeSale(basket.mapTo(HashSet()) { it.details.id }) {
                    showPaySheet = false
                }
            },
            onDismiss = { showPaySheet = false }
        )
    }

    if (showClearConfirmation) {
        StueberlConfirmationDialog(
            title = stringResource(R.string.till_clear_confirm_title),
            message = stringResource(R.string.till_clear_confirm_message),
            confirmLabel = stringResource(R.string.till_clear_basket),
            isDestructive = true,
            onConfirm = {
                showClearConfirmation = false
                viewModel.clearBasket()
            },
            onDismiss = { showClearConfirmation = false }
        )
    }
}

// ── Figures ────────────────────────────────────────────────────────────
@Composable
private fun TillFiguresCard(figures: TillFigures) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp)) {
            FigureCell(
                value = figures.revenue.format(),
                label = stringResource(R.string.till_figure_revenue),
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            FigureCell(
                value = figures.itemsSold.toString(),
                label = stringResource(R.string.till_figure_items),
                modifier = Modifier.weight(1f)
            )
            FigureCell(
                value = figures.customers.toString(),
                label = stringResource(R.string.till_figure_customers),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FigureCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) colorScheme.tertiary else colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Basket Row ─────────────────────────────────────────────────────────
@Composable
private fun BasketItemRow(
    product: Product,
    imageFile: File?,
    onRemove: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
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
            ProductThumbnail(imageFile = imageFile, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.details.description,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.details.salesPrice.format(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.tertiary
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.till_remove_item),
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private val TillMessage.isSuccess: Boolean
    get() = this is TillMessage.Added || this is TillMessage.Sold

private fun tillMessageText(context: Context, message: TillMessage): String? = when (message) {
    is TillMessage.Added ->
        if (message.description.isBlank()) context.getString(R.string.till_msg_added_generic)
        else context.getString(R.string.till_msg_added, message.description)
    TillMessage.ForeignCode -> context.getString(R.string.till_msg_foreign_code)
    TillMessage.OtherEvent -> context.getString(R.string.till_msg_other_event)
    TillMessage.ScannerUnavailable -> context.getString(R.string.till_msg_scanner_unavailable)
    is TillMessage.Sold -> context.getString(R.string.till_msg_sold, message.total.format())
    TillMessage.BasketChanged -> context.getString(R.string.till_msg_basket_changed)
    TillMessage.SaleNotRunning -> context.getString(R.string.till_msg_not_running)
    TillMessage.Failed -> context.getString(R.string.till_msg_failed)
    is TillMessage.NotAdded -> when (message.reason) {
        BasketAddResult.Added -> null
        BasketAddResult.EventNotRunning -> context.getString(R.string.till_msg_not_running)
        BasketAddResult.NotInEvent -> context.getString(R.string.till_msg_not_in_event)
        BasketAddResult.NotAvailable -> context.getString(R.string.till_msg_not_available)
        BasketAddResult.AlreadyInBasket -> context.getString(R.string.till_msg_already_in_basket)
    }
}