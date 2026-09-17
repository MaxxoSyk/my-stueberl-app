package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Int) -> Unit,
    productSaved: Boolean = false,
    onProductSavedConsumed: () -> Unit = {},
    productUpdated: Boolean = false,
    onProductUpdatedConsumed: () -> Unit = {}
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val selectedProducts by viewModel.selectedProducts.collectAsStateWithLifecycle()
    val selectedProductId by viewModel.selectedProductId.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()
    val isFilterSheetVisible by viewModel.isFilterSheetVisible.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    val selectedProduct = remember(selectedProductId, products) {
        selectedProductId?.let { id -> products.firstOrNull { it.details.id == id } }
    }

    val isSelectionMode = selectedProducts.isNotEmpty()

    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.products_screen_success_add)
    val updateMessage = stringResource(R.string.products_screen_success_edit)
    val errorMessage = stringResource(R.string.products_screen_action_failed)

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // ── Back Button ──
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                // ── Title ──
                title = {
                    Text(
                        text = if (isSelectionMode)
                            stringResource(R.string.products_screen_selected, selectedProducts.size)
                        else
                            stringResource(R.string.products_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                },
                // ── Action Icons ──
                actions = {
                    if (isSelectionMode) {
                        // ── Delete Icon ──
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.products_screen_delete),
                                tint = Color.White
                            )
                        }
                    } else {
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
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.products_screen_filter),
                                    tint = Color.White
                                )
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
            if (!isSelectionMode) {
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
        if (products.isEmpty()) {
            // ── Empty State ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (activeFilterCount > 0)
                        stringResource(R.string.products_screen_empty_filtered)
                    else
                        stringResource(R.string.products_screen_empty),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
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
                    items = products,
                    key = { it.details.id }
                ) { product ->
                    ProductDisplayItem(
                        product = product,
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
                        }
                    )
                }
            }
        }
    }

    // ── Delete Confirmation ────────────────────
    if (showDeleteConfirmation) {
        val count = selectedProducts.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(stringResource(R.string.products_screen_delete_confirm_title))
            },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.products_screen_delete_confirm_message,
                        count,
                        count
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteSelectedProducts()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.products_screen_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // ── Bottom Sheet ───────────────────────────
    selectedProduct?.let { product ->
        ProductBottomSheet(
            product = product,
            onMarkAsSold = {
                viewModel.markAsSold(product.details.id)
                viewModel.dismissBottomSheet()
            },
            onMarkAsRemoved = {
                viewModel.markAsRemoved(product.details.id)
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
}