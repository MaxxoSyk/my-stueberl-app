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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.viewmodel.product.ProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Int) -> Unit,
    productSaved: Boolean = false,
    onProductSavedConsumed: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val selectedProducts by viewModel.selectedProducts.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val isSelectionMode = selectedProducts.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.add_product_screen_success)

    // ── Show success message when product saved ──
    LaunchedEffect(productSaved) {
        if (productSaved) {
            snackbarHostState.showSnackbar(successMessage)
            onProductSavedConsumed()
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
                            contentDescription = "Back",
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
                        IconButton(onClick = {
                            viewModel.deleteSelectedProducts()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.products_screen_delete),
                                tint = Color.White
                            )
                        }
                    } else {
                        // ── Filter Icon ──
                        IconButton(onClick = { /* @todo Filter */ }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.products_screen_filter),
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
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { onNavigateToAddProduct() },
                    containerColor = SageGreen
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Product",
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
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.products_screen_empty),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
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

    // ── Bottom Sheet ───────────────────────────
    selectedProduct?.let { product ->
        ProductBottomSheet(
            product = product,
            onMarkAsSold = {
                viewModel.markAsSold(product.details.id)
            },
            onMarkAsRemoved = {
                viewModel.markAsRemoved(product.details.id)
            },
            onResetToAvailable = {
                viewModel.resetToAvailable(product.details.id)
            },
            onEdit = {
                viewModel.dismissBottomSheet()
                onNavigateToEditProduct(product.details.id)
            },
            onDismiss = {
                viewModel.dismissBottomSheet()
            }
        )
    }
}