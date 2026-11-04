package de.landstueberl.mystueberlapp.view.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.ProductListItem
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.viewmodel.product.ProductDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var previewFileName by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val current = state) {
            ProductDetailViewModel.State.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }

            ProductDetailViewModel.State.NotFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.product_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            is ProductDetailViewModel.State.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val item = current.item) {
                    is ProductListItem.Single -> {
                        val product = item.product
                        ProductDisplayItem(
                            product = product,
                            imageFile = remember(product) {
                                product.imageList.firstOrNull()
                                    ?.fileName
                                    ?.let(viewModel::imageFile)
                            },
                            isSelected = false,
                            isSelectionMode = false,
                            onTap = { },
                            onLongPress = { },
                            onImageTap = {
                                previewFileName = product.imageList.firstOrNull()?.fileName
                            }
                        )
                    }

                    is ProductListItem.Batch -> ProductBatchItem(
                        batch = item,
                        imageFileOf = { product ->
                            product.imageList.firstOrNull()
                                ?.fileName
                                ?.let(viewModel::imageFile)
                        },
                        // Arrived here specifically to see this batch.
                        isExpanded = true,
                        selectedProducts = emptySet(),
                        isSelectionMode = false,
                        onToggleExpanded = { },
                        onProductTap = { },
                        onProductLongPress = { },
                        onImageTap = { product ->
                            previewFileName = product.imageList.firstOrNull()?.fileName
                        }
                    )
                }
            }
        }
    }

    previewFileName?.let { fileName ->
        ProductImagePreviewDialog(
            imageFile = viewModel.imageFile(fileName),
            onDismiss = { previewFileName = null }
        )
    }
}