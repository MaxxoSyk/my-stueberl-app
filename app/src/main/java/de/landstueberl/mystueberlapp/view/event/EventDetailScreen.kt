package de.landstueberl.mystueberlapp.view.event

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.EventProducts
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.allProducts
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.label.LabelPrinter
import de.landstueberl.mystueberlapp.data.label.LabelSheetLayout
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import de.landstueberl.mystueberlapp.view.order.AttachProductDialog
import de.landstueberl.mystueberlapp.viewmodel.event.EventDetailUiState
import de.landstueberl.mystueberlapp.viewmodel.event.EventDetailViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.coroutines.cancellation.CancellationException

private val FOOTER_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (eventId: Int) -> Unit,
    onNavigateToEditProduct: (productId: Int) -> Unit,
    onNavigateToAddProduct: (eventId: Int) -> Unit,
    eventSaved: Boolean = false,
    onEventSavedConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val attachableItems by viewModel.attachableItems.collectAsStateWithLifecycle()
    val selectedForAttach by viewModel.selectedForAttach.collectAsStateWithLifecycle()
    val selectedForLabels by viewModel.selectedForLabels.collectAsStateWithLifecycle()
    val labelPrintedOn by viewModel.labelPrintedOn.collectAsStateWithLifecycle()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showStartConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    // IntArray, because it survives configuration changes in rememberSaveable.
    var pendingRemoval by rememberSaveable { mutableStateOf<IntArray?>(null) }
    var showAttachDialog by rememberSaveable { mutableStateOf(false) }
    var showLabelDialog by rememberSaveable { mutableStateOf(false) }
    var isPrintingLabels by remember { mutableStateOf(false) }

    // Products of the last print job, waiting for "Did it print?"
    var pendingPrintedIds by rememberSaveable { mutableStateOf<IntArray?>(null) }

    var showStartEventConfirmation by rememberSaveable { mutableStateOf(false) }

    // Activity context: PrintManager needs a window. Taken outside the Dialog on purpose.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labelErrorMessage = stringResource(R.string.label_print_failed)

    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.events_screen_success_save)
    val errorMessage = stringResource(R.string.event_detail_action_failed)

    LaunchedEffect(eventSaved) {
        if (eventSaved) {
            snackbarHostState.showSnackbar(savedMessage)
            onEventSavedConsumed()
        }
    }

    LaunchedEffect(isError) {
        if (isError) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetErrorState()
        }
    }

    val event = (uiState as? EventDetailUiState.Loaded)?.event

    val imageFileOf: (Product) -> File? = remember(viewModel) {
        { product -> product.imageList.firstOrNull()?.fileName?.let(viewModel::imageFile) }
    }

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
                    Text(
                        text = stringResource(R.string.event_detail_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                actions = {
                    if (event != null) {
                        IconButton(
                            onClick = { onNavigateToEdit(event.id) },
                            enabled = !isWorking
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.event_detail_edit)
                            )
                        }

                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.common_more)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (event.status.canPrintLabels && (products?.availableCount ?: 0) > 0) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.event_detail_print_labels)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.preselectUnprintedLabels()
                                            showLabelDialog = true
                                        }
                                    )
                                }

                                if (event.status.isCancellable) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.event_detail_cancel)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showCancelConfirmation = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.event_detail_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteConfirmation = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = StueberlDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            when (event?.status) {
                EventStatus.PLANNED -> EventBottomBar {
                    Button(
                        onClick = { showStartConfirmation = true },
                        enabled = !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = StueberlDefaults.primaryButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.event_detail_start_preparation),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Fixed at the bottom, so it stays reachable with long product lists.
                EventStatus.IN_PREPARATION -> EventBottomBar {
                    AddProductButtons(
                        onAddNew = { onNavigateToAddProduct(event.id) },
                        onAddExisting = {
                            viewModel.clearAttachSelection()
                            showAttachDialog = true
                        },
                        enabled = !isWorking
                    )

                    val hasAvailable = (products?.availableCount ?: 0) > 0
                    if (products != null && !hasAvailable) {
                        Text(
                            text = stringResource(R.string.event_detail_start_event_needs_products),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { showStartEventConfirmation = true },
                        enabled = hasAvailable && !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = StueberlDefaults.primaryButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.event_detail_start_event),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                else -> Unit
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = uiState) {
            EventDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            EventDetailUiState.NotFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.event_detail_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            is EventDetailUiState.Loaded -> {
                val isPreparing = state.event.status == EventStatus.IN_PREPARATION
                EventDetailContent(
                    event = state.event,
                    products = products,
                    imageFileOf = imageFileOf,
                    onProductClick = onNavigateToEditProduct,
                    // Products can only be added or removed while preparing.
                    onRemove = if (isPreparing) {
                        { ids -> pendingRemoval = ids.toIntArray() }
                    } else {
                        null
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    // ── Confirmations ──────────────────────────
    if (showStartConfirmation) {
        StueberlConfirmationDialog(
            title = stringResource(R.string.event_detail_start_preparation_confirm_title),
            message = stringResource(R.string.event_detail_start_preparation_confirm_message),
            confirmLabel = stringResource(R.string.event_detail_start_preparation),
            onConfirm = {
                showStartConfirmation = false
                viewModel.startPreparation()
            },
            onDismiss = { showStartConfirmation = false }
        )
    }

    if (showStartEventConfirmation && event != null) {
        val today = remember { LocalDate.now() }
        val unlabeledCount = products?.items.orEmpty().allProducts()
            .count { it.details.id !in labelPrintedOn }

        val base = stringResource(R.string.event_detail_start_event_confirm_message)
        val earlyNote = if (event.eventDate.isAfter(today)) {
            stringResource(
                R.string.event_detail_start_event_early,
                event.eventDate.format(FOOTER_DATE_FORMAT)
            )
        } else {
            null
        }
        val labelNote = if (unlabeledCount > 0) {
            pluralStringResource(
                R.plurals.event_detail_start_event_unlabeled,
                unlabeledCount,
                unlabeledCount
            )
        } else {
            null
        }

        StueberlConfirmationDialog(
            title = stringResource(R.string.event_detail_start_event_confirm_title),
            message = listOfNotNull(base, earlyNote, labelNote).joinToString("\n\n"),
            confirmLabel = stringResource(R.string.event_detail_start_event),
            onConfirm = {
                showStartEventConfirmation = false
                viewModel.startEvent()
            },
            onDismiss = { showStartEventConfirmation = false }
        )
    }

    if (showCancelConfirmation) {
        StueberlConfirmationDialog(
            title = stringResource(R.string.event_detail_cancel_confirm_title),
            message = stringResource(R.string.event_detail_cancel_confirm_message),
            confirmLabel = stringResource(R.string.event_detail_cancel),
            dismissLabel = stringResource(R.string.event_detail_cancel_dismiss),
            isDestructive = true,
            onConfirm = {
                showCancelConfirmation = false
                viewModel.cancelEvent()
            },
            onDismiss = { showCancelConfirmation = false }
        )
    }

    if (showDeleteConfirmation) {
        StueberlConfirmationDialog(
            title = stringResource(R.string.event_detail_delete_confirm_title),
            message = stringResource(R.string.event_detail_delete_confirm_message),
            confirmLabel = stringResource(R.string.event_detail_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteEvent(onDeleted = onNavigateBack)
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    pendingRemoval?.let { ids ->
        StueberlConfirmationDialog(
            title = stringResource(R.string.event_detail_remove_confirm_title),
            message = pluralStringResource(
                R.plurals.event_detail_remove_confirm_message,
                ids.size,
                ids.size
            ),
            confirmLabel = stringResource(R.string.event_detail_remove_product),
            onConfirm = {
                pendingRemoval = null
                viewModel.removeProducts(ids.toList())
            },
            onDismiss = { pendingRemoval = null }
        )
    }

    if (showAttachDialog) {
        AttachProductDialog(
            items = attachableItems,
            selectedIds = selectedForAttach,
            imageFileOf = imageFileOf,
            onToggleProduct = viewModel::toggleAttachSelection,
            onToggleBatch = viewModel::toggleBatchAttachSelection,
            onConfirm = {
                viewModel.attachSelectedProducts { showAttachDialog = false }
            },
            onDismiss = {
                viewModel.clearAttachSelection()
                showAttachDialog = false
            },
            emptyText = stringResource(R.string.event_attach_empty)
        )
    }

    if (showLabelDialog) {
        val jobName = stringResource(R.string.label_print_job, event?.name.orEmpty())
        LabelSelectionDialog(
            items = products?.items.orEmpty(),
            selectedIds = selectedForLabels,
            printedOn = labelPrintedOn,
            imageFileOf = imageFileOf,
            isPrinting = isPrintingLabels,
            onToggleProduct = viewModel::toggleLabelSelection,
            onToggleBatch = viewModel::toggleBatchLabelSelection,
            onSelectAll = viewModel::selectAllForLabels,
            onSelectNone = viewModel::clearLabelSelection,
            onPrint = {
                val labels = viewModel.selectedLabels()
                if (labels.isNotEmpty() && !isPrintingLabels) {
                    scope.launch {
                        isPrintingLabels = true
                        try {
                            val pdf = LabelPrinter.createPdf(context, labels)
                            LabelPrinter.print(
                                context = context,
                                pdf = pdf,
                                pageCount = LabelSheetLayout.pageCount(labels.size),
                                jobName = jobName
                            )
                            // The print screen covers the app; this question is
                            // waiting when the user comes back from it.
                            pendingPrintedIds = labels.map { it.code.productId }.toIntArray()
                            showLabelDialog = false
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("EventDetailScreen", "Failed to create label sheet", e)
                            scope.launch { snackbarHostState.showSnackbar(labelErrorMessage) }
                        } finally {
                            isPrintingLabels = false
                        }
                    }
                }
            },
            onDismiss = { showLabelDialog = false }
        )
    }

    pendingPrintedIds?.let { ids ->
        StueberlConfirmationDialog(
            title = stringResource(R.string.label_print_confirm_title),
            message = pluralStringResource(
                R.plurals.label_print_confirm_message,
                ids.size,
                ids.size
            ),
            confirmLabel = stringResource(R.string.label_print_confirm_yes),
            dismissLabel = stringResource(R.string.label_print_confirm_no),
            onConfirm = {
                pendingPrintedIds = null
                viewModel.markLabelsPrinted(ids.toList())
            },
            onDismiss = { pendingPrintedIds = null }
        )
    }
}

// ── Content ────────────────────────────────────────────────────────────
@Composable
private fun EventDetailContent(
    event: EventDetail,
    products: EventProducts?,
    imageFileOf: (Product) -> File?,
    onProductClick: (Int) -> Unit,
    onRemove: ((List<Int>) -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Keyed by status: collapses by itself when preparation or the event starts,
    // so the product list is in view.
    var isInfoExpanded by rememberSaveable(event.status) {
        mutableStateOf(
            event.status != EventStatus.IN_PREPARATION && event.status != EventStatus.IN_PROGRESS
        )
    }
    val expandedBatchIds = remember { mutableStateSetOf<Int>() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "status") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                EventStatusBadge(status = event.status)
            }
        }

        item(key = "info") {
            EventInfoCard(
                event = event,
                isExpanded = isInfoExpanded,
                onToggle = { isInfoExpanded = !isInfoExpanded }
            )
        }

        if (event.status == EventStatus.PLANNED) {
            item(key = "planned_hint") {
                HintText(stringResource(R.string.event_detail_planned_hint))
            }
        } else if (products != null) {
            if (event.status == EventStatus.IN_PROGRESS) {
                item(key = "in_progress_hint") {
                    HintText(stringResource(R.string.event_detail_in_progress_hint))
                }
            }
            eventProductSections(
                products = products,
                imageFileOf = imageFileOf,
                expandedBatchIds = expandedBatchIds,
                onProductClick = onProductClick,
                onRemove = onRemove
            )
        }

        item(key = "footer") {
            EventFooter(event = event)
        }
    }
}

private fun LazyListScope.eventProductSections(
    products: EventProducts,
    imageFileOf: (Product) -> File?,
    expandedBatchIds: MutableSet<Int>,
    onProductClick: (Int) -> Unit,
    onRemove: ((List<Int>) -> Unit)?
) {
    item(key = "header_products") {
        DetailSectionHeader(stringResource(R.string.event_detail_section_products))
    }

    if (products.items.isEmpty()) {
        item(key = "no_products") {
            HintText(stringResource(R.string.event_detail_no_products))
        }
    } else {
        item(key = "totals") {
            EventTotals(products = products)
        }
        items(items = products.items, key = { it.key }) { item ->
            when (item) {
                is ProductListItem.Single -> EventProductItem(
                    product = item.product,
                    imageFile = imageFileOf(item.product),
                    onClick = { onProductClick(item.product.details.id) },
                    onRemove = onRemove?.let { remove -> { remove(listOf(item.product.details.id)) } }
                )

                is ProductListItem.Batch -> EventProductBatchItem(
                    batch = item,
                    imageFileOf = imageFileOf,
                    isExpanded = item.batchId in expandedBatchIds,
                    onToggleExpanded = {
                        if (!expandedBatchIds.add(item.batchId)) expandedBatchIds.remove(item.batchId)
                    },
                    onProductClick = { onProductClick(it.details.id) },
                    onRemoveBatch = onRemove?.let { remove ->
                        { remove(item.products.map { it.details.id }) }
                    },
                    onRemoveProduct = onRemove?.let { remove ->
                        { product: Product -> remove(listOf(product.details.id)) }
                    }
                )
            }
        }
    }

    if (products.noLongerAvailable.isNotEmpty()) {
        item(key = "header_gone") {
            DetailSectionHeader(
                text = stringResource(R.string.event_detail_section_no_longer_available),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(
            items = products.noLongerAvailable,
            key = { "gone_${it.details.id}" }
        ) { product ->
            EventProductItem(
                product = product,
                imageFile = imageFileOf(product),
                onClick = { onProductClick(product.details.id) },
                onRemove = onRemove?.let { remove -> { remove(listOf(product.details.id)) } },
                showStatus = true
            )
        }
    }
}

// ── Info Card ──────────────────────────────────────────────────────────
@Composable
private fun EventInfoCard(
    event: EventDetail,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = eventDateText(event),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.event_detail_hide_details else R.string.event_detail_show_details
                    ),
                    tint = colorScheme.secondary
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = colorScheme.primary.copy(alpha = 0.3f))

                event.location?.takeIf { it.isNotBlank() }?.let { location ->
                    DetailRow(
                        icon = Icons.Default.Place,
                        label = stringResource(R.string.event_form_location),
                        value = location
                    )
                }

                val fee = event.standFee
                DetailRow(
                    icon = Icons.Default.Payments,
                    label = stringResource(R.string.event_form_stand_fee),
                    value = when {
                        fee == null -> stringResource(R.string.event_detail_stand_fee_unknown)
                        fee.amount.signum() == 0 -> stringResource(R.string.event_detail_stand_fee_free)
                        else -> fee.format()
                    },
                    valueColor = if (fee == null) colorScheme.onSurfaceVariant else colorScheme.tertiary
                )

                event.note?.takeIf { it.isNotBlank() }?.let { note ->
                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = stringResource(R.string.event_form_note),
                        value = note
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor
            )
        }
    }
}

// ── Totals, Headers, Footer ────────────────────────────────────────────
/** Only available products count: what is actually taken along. */
@Composable
private fun EventTotals(products: EventProducts) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.event_detail_total),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onBackground
            )
            Text(
                text = pluralStringResource(
                    R.plurals.events_screen_product_count,
                    products.availableCount,
                    products.availableCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = products.salesTotal.format(),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.tertiary
            )
            Text(
                text = stringResource(
                    R.string.event_detail_purchase_price,
                    products.purchaseTotal.format()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

/** "New" and "Existing", side by side. */
@Composable
private fun AddProductButtons(
    onAddNew: () -> Unit,
    onAddExisting: () -> Unit,
    enabled: Boolean = true
) {
    val buttonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.secondary
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onAddNew,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = buttonColors
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.event_detail_add_new),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = onAddExisting,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = buttonColors
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.event_detail_add_existing),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * Bottom bar that sizes itself to its content (BottomAppBar is fixed at
 * 80 dp, too small for two rows) and keeps clear of the navigation bar.
 */
@Composable
private fun EventBottomBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun EventFooter(event: EventDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                R.string.event_detail_created_at,
                event.createdAt.format(FOOTER_DATE_FORMAT)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        event.cancelledOn?.let { cancelledOn ->
            Text(
                text = stringResource(
                    R.string.event_detail_cancelled_on,
                    cancelledOn.format(FOOTER_DATE_FORMAT)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}