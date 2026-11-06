package de.landstueberl.mystueberlapp.view.product

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.data.ProductSortOrder
import de.landstueberl.mystueberlapp.data.labelRes
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Int) -> Unit,
    onNavigateToOrder: (Int) -> Unit,
    productSaved: Boolean = false,
    onProductSavedConsumed: () -> Unit = {},
    productUpdated: Boolean = false,
    onProductUpdatedConsumed: () -> Unit = {}
) {
    val selectedProducts by viewModel.selectedProducts.collectAsStateWithLifecycle()
    val selectedProductId by viewModel.selectedProductId.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()
    val isFilterSheetVisible by viewModel.isFilterSheetVisible.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val isFilterNarrowing by viewModel.isFilterNarrowing.collectAsStateWithLifecycle()
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    // Collapses when leaving the screen — plain remember, not rememberSaveable.
    val expandedBatchIds = remember { mutableStateSetOf<Int>() }

    val selectedProduct = remember(selectedProductId, listItems) {
        selectedProductId?.let { id ->
            listItems.asSequence()
                .flatMap { item ->
                    when (item) {
                        is ProductListItem.Single -> sequenceOf(item.product)
                        is ProductListItem.Batch -> item.products.asSequence()
                    }
                }
                .firstOrNull { it.details.id == id }
        }
    }

    val isSelectionMode = selectedProducts.isNotEmpty()

    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.products_screen_success_add)
    val updateMessage = stringResource(R.string.products_screen_success_edit)
    val errorMessage = stringResource(R.string.products_screen_action_failed)
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var previewImageName by rememberSaveable { mutableStateOf<String?>(null) }

    // Focus the field as it appears, so you can type immediately.
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    // ── Show success message when product saved ──
    LaunchedEffect(productSaved) {
        if (productSaved) {
            snackbarHostState.showSnackbar(successMessage)
            onProductSavedConsumed()
        }
    }

    // ── Show update message when product updated ──
    LaunchedEffect(productUpdated) {
        if (productUpdated) {
            snackbarHostState.showSnackbar(updateMessage)
            onProductUpdatedConsumed()
        }
    }

    // ── Show error message when a DB action failed ──
    LaunchedEffect(isError) {
        if (isError) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetErrorState()
        }
    }

    BackHandler(enabled = isSearchActive) {
        viewModel.closeSearch()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            isSearchActive -> viewModel.closeSearch()
                            isSelectionMode -> viewModel.clearSelection()
                            else -> onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) {
                                Icons.Default.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = stringResource(
                                if (isSearchActive) R.string.common_cancel
                                else R.string.common_back
                            ),
                            tint = Color.White
                        )
                    }
                },
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.products_screen_search_hint),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )
                    } else {
                        Text(
                            text = if (isSelectionMode) {
                                stringResource(
                                    R.string.products_screen_selected,
                                    selectedProducts.size
                                )
                            } else {
                                stringResource(R.string.products_screen_title)
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    when {
                        // No actions while searching — sort and filter don't apply
                        isSearchActive -> Unit

                        isSelectionMode -> {
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.products_screen_delete),
                                    tint = Color.White
                                )
                            }
                        }

                        else -> {
                            // ── Search Icon ──
                            IconButton(onClick = { viewModel.openSearch() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.products_screen_search),
                                    tint = Color.White
                                )
                            }

                            // ── Sort Menu ──
                            Box {
                                IconButton(onClick = { sortMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = stringResource(R.string.products_screen_sort),
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    ProductSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(order.labelRes)) },
                                            onClick = {
                                                viewModel.setSortOrder(order)
                                                sortMenuExpanded = false
                                            },
                                            trailingIcon = {
                                                if (order == sortOrder) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = SageGreenDark
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // ── Filter Icon ──
                            IconButton(onClick = { viewModel.showFilterSheet() }) {
                                BadgedBox(
                                    badge = {
                                        if (activeFilterCount > 0) {
                                            Badge(
                                                containerColor = SageGreenDark
                                            ) {
                                                Text(
                                                    text = activeFilterCount.toString(),
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterAlt,
                                        contentDescription = stringResource(R.string.products_screen_filter),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode && !isSearchActive) {
                FloatingActionButton(
                    onClick = { onNavigateToAddProduct() },
                    containerColor = SageGreen
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.products_screen_add),
                        tint = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (listItems.isEmpty()) {
            // ── Empty State ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isSearchActive && searchQuery.isNotBlank() ->
                            stringResource(R.string.products_screen_search_no_results)
                        isSearchActive ->
                            stringResource(R.string.products_screen_search_prompt)
                        isFilterNarrowing ->
                            stringResource(R.string.products_screen_empty_filtered)
                        else ->
                            stringResource(R.string.products_screen_empty)
                    },
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // ── Product List ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = listItems,
                    key = { it.key }
                ) { item ->
                    when (item) {
                        is ProductListItem.Single -> {
                            val product = item.product
                            ProductDisplayItem(
                                product = product,
                                imageFile = remember(product) {
                                    product.imageList.firstOrNull()
                                        ?.fileName
                                        ?.let(viewModel::imageFile)
                                },
                                isSelected = selectedProducts.contains(product.details.id),
                                isSelectionMode = isSelectionMode,
                                onTap = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(product.details.id)
                                    } else {
                                        viewModel.onProductTap(product)
                                    }
                                },
                                onLongPress = {
                                    viewModel.toggleSelection(product.details.id)
                                },
                                onImageTap = {
                                    previewImageName =
                                        product.imageList.firstOrNull()?.fileName
                                }
                            )
                        }

                        is ProductListItem.Batch -> {
                            ProductBatchItem(
                                batch = item,
                                imageFileOf = { product ->
                                    product.imageList.firstOrNull()
                                        ?.fileName
                                        ?.let(viewModel::imageFile)
                                },
                                isExpanded = expandedBatchIds.contains(item.batchId),
                                selectedProducts = selectedProducts,
                                isSelectionMode = isSelectionMode,
                                onToggleExpanded = {
                                    if (!expandedBatchIds.add(item.batchId)) {
                                        expandedBatchIds.remove(item.batchId)
                                    }
                                },
                                onProductTap = { product ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(product.details.id)
                                    } else {
                                        viewModel.onProductTap(product)
                                    }
                                },
                                onProductLongPress = { product ->
                                    viewModel.toggleSelection(product.details.id)
                                },
                                onImageTap = { product ->
                                    previewImageName =
                                        product.imageList.firstOrNull()?.fileName
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation ────────────────────
    if (showDeleteConfirmation) {
        val count = selectedProducts.size
        StueberlConfirmationDialog(
            title = stringResource(R.string.products_screen_delete_confirm_title),
            message = pluralStringResource(
                R.plurals.products_screen_delete_confirm_message,
                count,
                count
            ),
            confirmLabel = stringResource(R.string.products_screen_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteSelectedProducts()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    // ── Bottom Sheet ───────────────────────────
    selectedProduct?.let { product ->
        ProductBottomSheet(
            product = product,
            onMarkAsSold = { soldOn ->
                viewModel.markAsSold(product.details.id, soldOn)
                viewModel.dismissBottomSheet()
            },
            onMarkAsRemoved = { removedOn ->
                viewModel.markAsRemoved(product.details.id, removedOn)
                viewModel.dismissBottomSheet()
            },
            onResetToAvailable = {
                viewModel.resetToAvailable(product.details.id)
                viewModel.dismissBottomSheet()
            },
            onEdit = {
                viewModel.dismissBottomSheet()
                onNavigateToEditProduct(product.details.id)
            },
            onGoToOrder = {
                viewModel.dismissBottomSheet()
                product.details.orderId?.let(onNavigateToOrder)
            },
            onDismiss = { viewModel.dismissBottomSheet() }
        )
    }

    // ── Filter Sheet ───────────────────────────
    if (isFilterSheetVisible) {
        FilterBottomSheet(
            filter = filter,
            availableYears = availableYears,
            onYearToggle = { viewModel.toggleYearFilter(it) },
            onStatusToggle = { viewModel.toggleStatusFilter(it) },
            onProductSourceFilterChange = { viewModel.setOrderFilter(it) },
            onReset = { viewModel.resetFilter() },
            onApply = { viewModel.hideFilterSheet() },
            onDismiss = { viewModel.hideFilterSheet() }
        )
    }

    previewImageName?.let { fileName ->
        ProductImagePreviewDialog(
            imageFile = viewModel.imageFile(fileName),
            onDismiss = { previewImageName = null }
        )
    }
}