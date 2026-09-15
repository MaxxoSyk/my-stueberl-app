package de.landstueberl.mystueberlapp.view.product.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: EditProductViewModel,
    onNavigateBack: (productUpdated: Boolean) -> Unit
) {
    val description by viewModel.description.collectAsStateWithLifecycle()
    val purchasePrice by viewModel.purchasePrice.collectAsStateWithLifecycle()
    val salesPrice by viewModel.salesPrice.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val createdAt by viewModel.createdAt.collectAsStateWithLifecycle()
    val removedOn by viewModel.removedOn.collectAsStateWithLifecycle()
    val isSold by viewModel.isSold.collectAsStateWithLifecycle()
    val isRemoved by viewModel.isRemoved.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.product_form_save_failed)

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
            snackbarHostState.showSnackbar(saveErrorMessage)
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
                            contentDescription = stringResource(R.string.common_back),
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
        when {
            // ── Loading State ──────────────────
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }

            // ── Load Failed State ──────────────
            loadFailed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.edit_product_screen_load_failed),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { viewModel.retry() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.edit_product_screen_retry))
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProductImagePlaceholder(
                    onClick = { /* @todo Image picker */ }
                )

                ProductStatusBadge(
                    isSold = isSold,
                    isRemoved = isRemoved
                )

                ProductDescriptionField(
                    value = description,
                    onValueChange = viewModel::onDescriptionChange
                )

                ProductPriceField(
                    label = stringResource(R.string.add_product_screen_purchase_price),
                    value = purchasePrice,
                    onValueChange = viewModel::onPurchasePriceChange,
                    currency = currency.currencyCode,
                    availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                    onCurrencyChange = { code ->
                        viewModel.onCurrencyChange(Currency.getInstance(code))
                    }
                )

                ProductPriceField(
                    label = stringResource(R.string.add_product_screen_sales_price),
                    value = salesPrice,
                    onValueChange = viewModel::onSalesPriceChange,
                    currency = currency.currencyCode,
                    availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                    onCurrencyChange = { code ->
                        viewModel.onCurrencyChange(Currency.getInstance(code))
                    }
                )

                // ── Read Only Section ──────────
                ProductFormDivider(
                    label = stringResource(R.string.edit_product_screen_section_details)
                        .uppercase(Locale.ROOT)
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

                // ── Save Button ────────────────
                Button(
                    onClick = { viewModel.saveProduct() },
                    enabled = isSaveEnabled && !isSaving,
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
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
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
}