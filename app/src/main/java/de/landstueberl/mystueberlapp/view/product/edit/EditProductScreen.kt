package de.landstueberl.mystueberlapp.view.product.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.view.product.ProductDescriptionField
import de.landstueberl.mystueberlapp.view.product.ProductFormDivider
import de.landstueberl.mystueberlapp.view.product.ProductImagePlaceholder
import de.landstueberl.mystueberlapp.view.product.ProductPriceField
import de.landstueberl.mystueberlapp.view.product.ProductReadOnlyDateField
import de.landstueberl.mystueberlapp.view.product.ProductStatusBadge
import de.landstueberl.mystueberlapp.viewmodel.product.edit.EditProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: EditProductViewModel,
    onNavigateBack: (productUpdated: Boolean) -> Unit
) {
    val description by viewModel.description.collectAsState()
    val purchasePrice by viewModel.purchasePrice.collectAsState()
    val salesPrice by viewModel.salesPrice.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val createdAt by viewModel.createdAt.collectAsState()
    val removedOn by viewModel.removedOn.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val isError by viewModel.isError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.add_product_screen_price_invalid)

    // ── Handle Save Success ────────────────────
    LaunchedEffect(isSaved) {
        if (isSaved) {
            viewModel.resetSavedState()
            onNavigateBack(true)
        }
    }

    // ── Handle Error ───────────────────────────
    LaunchedEffect(isError) {
        if (isError) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetErrorState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.edit_product_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            // ── Loading State ──────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }
        } else {
            // ── Form Content ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isSold by viewModel.isSold.collectAsState()
                val isRemoved by viewModel.isRemoved.collectAsState()

                // ── Image Placeholder ──────────
                ProductImagePlaceholder(
                    onClick = { /* @todo Image picker */ }
                )

                // ── Status Badge ───────────────────────────
                ProductStatusBadge(
                    isSold = isSold,
                    isRemoved = isRemoved
                )

                // ── Description ───────────────
                ProductDescriptionField(
                    value = description,
                    onValueChange = { viewModel.onDescriptionChange(it) }
                )

                // ── Purchase Price ─────────────
                ProductPriceField(
                    label = stringResource(R.string.add_product_screen_purchase_price),
                    value = purchasePrice,
                    onValueChange = { viewModel.onPurchasePriceChange(it) },
                    currency = currency.currencyCode,
                    availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                    onCurrencyChange = { code ->
                        viewModel.onCurrencyChange(java.util.Currency.getInstance(code))
                    }
                )

                // ── Sales Price ────────────────
                ProductPriceField(
                    label = stringResource(R.string.add_product_screen_sales_price),
                    value = salesPrice,
                    onValueChange = { viewModel.onSalesPriceChange(it) },
                    currency = currency.currencyCode,
                    availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                    onCurrencyChange = { code ->
                        viewModel.onCurrencyChange(java.util.Currency.getInstance(code))
                    }
                )

                // ── Read Only Dates ────────────
                ProductFormDivider(
                    label = stringResource(R.string.edit_product_screen_created_at)
                        .uppercase()
                )

                ProductReadOnlyDateField(
                    label = stringResource(R.string.edit_product_screen_created_at),
                    date = createdAt
                )

                if (removedOn != null) {
                    ProductReadOnlyDateField(
                        label = stringResource(R.string.edit_product_screen_removed_on),
                        date = removedOn
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Save Button ────────────────
                Button(
                    onClick = { viewModel.saveProduct() },
                    enabled = viewModel.isSaveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White,
                        disabledContainerColor = SageGreenLight,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.add_product_screen_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}