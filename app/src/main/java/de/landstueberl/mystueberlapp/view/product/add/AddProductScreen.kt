package de.landstueberl.mystueberlapp.view.product.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onNavigateBack: (showSuccess: Boolean) -> Unit
) {
    val description by viewModel.description.collectAsState()
    val purchasePrice by viewModel.purchasePrice.collectAsState()
    val salesPrice by viewModel.salesPrice.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val isError by viewModel.isError.collectAsState()

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
                        text = stringResource(R.string.add_product_screen_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Image Placeholder ──────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SageGreenLight)
                    .border(
                        width = 2.dp,
                        color = SageGreen,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { /* @todo Image picker */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = stringResource(R.string.add_product_screen_image_placeholder),
                        tint = SageGreenDark,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_product_screen_image_placeholder),
                        fontSize = 11.sp,
                        color = SageGreenDark
                    )
                }
            }

            // ── Description Field ──────────────
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = {
                    Text(stringResource(R.string.add_product_screen_description))
                },
                placeholder = {
                    Text(
                        stringResource(R.string.add_product_screen_description_placeholder),
                        color = TextSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SageGreen,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = SageGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = SageGreen
                )
            )

            // ── Purchase Price + Currency ──────
            PriceInputField(
                label = stringResource(R.string.add_product_screen_purchase_price),
                value = purchasePrice,
                onValueChange = { viewModel.onPurchasePriceChange(it) },
                currency = currency.currencyCode,
                availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                onCurrencyChange = { code ->
                    viewModel.onCurrencyChange(
                        java.util.Currency.getInstance(code)
                    )
                }
            )

            // ── Sales Price + Currency ─────────
            PriceInputField(
                label = stringResource(R.string.add_product_screen_sales_price),
                value = salesPrice,
                onValueChange = { viewModel.onSalesPriceChange(it) },
                currency = currency.currencyCode,
                availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                onCurrencyChange = { code ->
                    viewModel.onCurrencyChange(
                        java.util.Currency.getInstance(code)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save Button ────────────────────
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

// ── Price Input Field ──────────────────────────────────────────────────
@Composable
private fun PriceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currency: String,
    availableCurrencies: List<String>,
    onCurrencyChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Price TextField ──
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SageGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = SageGreen,
                unfocusedLabelColor = TextSecondary,
                cursorColor = SageGreen
            ),
            singleLine = true
        )

        // ── Currency Dropdown ──
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = TextSecondary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currency,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableCurrencies.forEach { code ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = code,
                                color = if (code == currency)
                                    SageGreenDark
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onCurrencyChange(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}