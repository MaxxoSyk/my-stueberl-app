package de.landstueberl.mystueberlapp.view.event

import android.content.Context
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.BasketAddResult
import de.landstueberl.mystueberlapp.data.BasketLine
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.TillFigures
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.label.ProductScanner
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import de.landstueberl.mystueberlapp.view.common.rememberTimedReveal
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
    val recentSales by viewModel.recentSales.collectAsStateWithLifecycle()
    val basketLines by viewModel.basketLines.collectAsStateWithLifecycle()
    val batchesWithMore by viewModel.batchesWithMore.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showPaySheet by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSalesSheet by rememberSaveable { mutableStateOf(false) }
    var saleToUndo by rememberSaveable { mutableStateOf<Int?>(null) }

    // Figures are hidden by default: the screen stays on, and anyone at the
    // stand could read them. Plain remember, so they're hidden again whenever
    // the till is reopened.
    var figuresRevealed by rememberTimedReveal()

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

    LaunchedEffect(recentSales.isEmpty()) {
        if (recentSales.isEmpty()) showSalesSheet = false
    }

    // The till is used all day at the stand: don't let the screen time out.
    val view = LocalView.current
    DisposableEffect(view, isRunning) {
        view.keepScreenOn = isRunning
        onDispose { view.keepScreenOn = false }
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
                    if (recentSales.isNotEmpty()) {
                        IconButton(onClick = { showSalesSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = stringResource(R.string.till_sales_title)
                            )
                        }
                    }
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

                item(key = "figures") {
                    TillFiguresCard(
                        figures = figures,
                        isRevealed = figuresRevealed,
                        onToggle = { figuresRevealed = !figuresRevealed }
                    )
                }

                item(key = "basket_header") {
                    DetailSectionHeader(
                        text = stringResource(R.string.till_basket, basket.size),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (basket.isEmpty()) {
                    item(key = "basket_empty") { HintText(stringResource(R.string.till_basket_empty)) }
                } else {
                    items(items = basketLines, key = { it.key }) { line ->
                        when (line) {
                            is BasketLine.Single -> BasketItemRow(
                                product = line.product,
                                imageFile = imageFileOf(line.product),
                                onRemove = if (isRunning) {
                                    { viewModel.removeFromBasket(line.product.details.id) }
                                } else {
                                    null
                                }
                            )

                            is BasketLine.Batch -> BasketBatchRow(
                                line = line,
                                imageFile = imageFileOf(line.representative),
                                enabled = isRunning && !isWorking,
                                canAddMore = line.batchId in batchesWithMore,
                                onAdd = { viewModel.addNextOfBatch(line.batchId) },
                                onRemoveOne = { viewModel.removeNewestOfBatch(line.batchId) }
                            )
                        }
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
            basket = basket,
            basketTotal = basketTotal,
            imageFileOf = imageFileOf,
            onQueryChange = viewModel::onSearchQueryChange,
            onAdd = viewModel::addToBasket,
            onRemove = viewModel::removeFromBasket,
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

    if (showSalesSheet) {
        TillSalesSheet(
            sales = recentSales,
            isWorking = isWorking,
            // Undo only while the event is running.
            onUndo = if (isRunning) { saleId -> saleToUndo = saleId } else null,
            onDismiss = { showSalesSheet = false }
        )
    }

    saleToUndo?.let { saleId ->
        val entry = recentSales.firstOrNull { it.sale.id == saleId }
        if (entry == null) {
            // Undone or gone meanwhile: nothing to confirm.
            LaunchedEffect(saleId) { saleToUndo = null }
        } else {
            StueberlConfirmationDialog(
                title = stringResource(R.string.till_undo_confirm_title),
                message = pluralStringResource(
                    R.plurals.till_undo_confirm_message,
                    entry.sale.itemCount,
                    entry.sale.itemCount,
                    entry.sale.total.format()
                ),
                confirmLabel = stringResource(R.string.till_undo),
                isDestructive = true,
                onConfirm = {
                    saleToUndo = null
                    // Undoing several sales in a row is rare: back to the till,
                    // where the "Undone: …" snackbar is visible.
                    showSalesSheet = false
                    viewModel.undoSale(saleId)
                },
                onDismiss = { saleToUndo = null }
            )
        }
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
/** Placeholder while hidden; same text style, so the card doesn't jump. */
private const val HIDDEN_VALUE = "••••"

@Composable
private fun TillFiguresCard(
    figures: TillFigures,
    isRevealed: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FigureCell(
                value = if (isRevealed) figures.revenue.format() else HIDDEN_VALUE,
                label = stringResource(R.string.till_figure_revenue),
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            FigureCell(
                value = if (isRevealed) figures.itemsSold.toString() else HIDDEN_VALUE,
                label = stringResource(R.string.till_figure_items),
                modifier = Modifier.weight(1f)
            )
            FigureCell(
                value = if (isRevealed) figures.customers.toString() else HIDDEN_VALUE,
                label = stringResource(R.string.till_figure_customers),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = stringResource(
                    if (isRevealed) R.string.till_figures_hide else R.string.till_figures_show
                ),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
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
        AnimatedContent(targetState = value, label = "figure") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) colorScheme.tertiary else colorScheme.onSurface,
                maxLines = 1
            )
        }
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

/** All basket members of one batch, with a counter instead of a remove icon. */
@Composable
private fun BasketBatchRow(
    line: BasketLine.Batch,
    imageFile: File?,
    enabled: Boolean,
    canAddMore: Boolean,
    onAdd: () -> Unit,
    onRemoveOne: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val details = line.representative.details

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
                    text = details.description,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.till_basket_unit_line,
                        line.count,
                        details.salesPrice.format()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = line.total.format(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.tertiary
                )
            }

            // − n +
            IconButton(onClick = onRemoveOne, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.till_add_sheet_remove_one),
                    tint = colorScheme.onSurfaceVariant
                )
            }
            AnimatedContent(targetState = line.count, label = "batchCount") { count ->
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 24.dp)
                )
            }
            IconButton(onClick = onAdd, enabled = enabled && canAddMore) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = stringResource(R.string.till_basket_add_one),
                    tint = if (enabled && canAddMore) colorScheme.secondary
                    else colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

private val TillMessage.isSuccess: Boolean
    get() = this is TillMessage.Added || this is TillMessage.Sold || this is TillMessage.Undone

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
        BasketAddResult.BatchEmpty -> context.getString(R.string.till_msg_batch_empty)
    }
    is TillMessage.Undone -> context.getString(R.string.till_msg_undone, message.total.format())
}