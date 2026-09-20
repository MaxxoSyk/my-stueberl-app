package de.landstueberl.mystueberlapp.view.product.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import de.landstueberl.mystueberlapp.view.product.ImageSourceBottomSheet
import de.landstueberl.mystueberlapp.view.product.ProductDescriptionField
import de.landstueberl.mystueberlapp.view.product.ProductImagePlaceholder
import de.landstueberl.mystueberlapp.view.product.ProductPriceField
import de.landstueberl.mystueberlapp.viewmodel.product.add.AddProductViewModel
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onNavigateBack: (showSuccess: Boolean) -> Unit
) {
    val description by viewModel.description.collectAsStateWithLifecycle()
    val purchasePrice by viewModel.purchasePrice.collectAsStateWithLifecycle()
    val salesPrice by viewModel.salesPrice.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.product_form_save_failed)

    LaunchedEffect(isSaved) {
        if (isSaved) {
            viewModel.resetSavedState()
            onNavigateBack(true)
        }
    }

    LaunchedEffect(isError) {
        if (isError) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            viewModel.resetErrorState()
        }
    }

    val imageFileName by viewModel.imageFileName.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    var showImageSourceSheet by rememberSaveable { mutableStateOf(false) }

    // Target URI the camera writes into; kept across the launch.
    var cameraTargetUri by remember { mutableStateOf<Uri?>(null) }

    // ── Gallery ────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onImageSelected(uri)
    }

    // ── Camera ─────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val target = cameraTargetUri
        if (success && target != null) viewModel.onImageSelected(target)
        cameraTargetUri = null
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
            ProductImagePlaceholder(
                imageFile = imageFileName?.let { viewModel.imageFile(it) },
                isLoading = isImporting,
                onClick = { showImageSourceSheet = true }
            )

            // ── Description ───────────────────
            ProductDescriptionField(
                value = description,
                onValueChange = viewModel::onDescriptionChange
            )

            // ── Purchase Price ─────────────────
            ProductPriceField(
                label = stringResource(R.string.product_form_purchase_price),
                value = purchasePrice,
                onValueChange = viewModel::onPurchasePriceChange,
                currency = currency.currencyCode,
                availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                onCurrencyChange = { code ->
                    viewModel.onCurrencyChange(Currency.getInstance(code))
                }
            )

            // ── Sales Price ────────────────────
            ProductPriceField(
                label = stringResource(R.string.product_form_sales_price),
                value = salesPrice,
                onValueChange = viewModel::onSalesPriceChange,
                currency = currency.currencyCode,
                availableCurrencies = viewModel.availableCurrencies.map { it.currencyCode },
                onCurrencyChange = { code ->
                    viewModel.onCurrencyChange(Currency.getInstance(code))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save Button ────────────────────
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
                        text = stringResource(R.string.product_form_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showImageSourceSheet) {
        ImageSourceBottomSheet(
            canRemove = imageFileName != null,
            onTakePhoto = {
                showImageSourceSheet = false
                val target = viewModel.createCameraTarget()
                cameraTargetUri = target.uri
                cameraLauncher.launch(target.uri)
            },
            onPickFromGallery = {
                showImageSourceSheet = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = {
                showImageSourceSheet = false
                viewModel.onImageRemoved()
            },
            onDismiss = { showImageSourceSheet = false }
        )
    }
}