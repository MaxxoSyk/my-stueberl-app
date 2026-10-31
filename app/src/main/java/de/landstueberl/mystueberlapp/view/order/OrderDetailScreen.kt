package de.landstueberl.mystueberlapp.view.order

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.data.orderTotal
import de.landstueberl.mystueberlapp.ui.theme.AccentGold
import de.landstueberl.mystueberlapp.ui.theme.FloralWhite
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenDark
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.util.toLocalDateFromPicker
import de.landstueberl.mystueberlapp.util.toPickerMillis
import de.landstueberl.mystueberlapp.view.product.ProductFormDivider
import de.landstueberl.mystueberlapp.view.product.ProductReadOnlyDateField
import de.landstueberl.mystueberlapp.viewmodel.order.OrderDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    onNavigateBack: (orderSaved: Boolean) -> Unit,
    onNavigateToAddProduct: (orderId: Int) -> Unit,
    onNavigateToEditProduct: (productId: Int) -> Unit
) {
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDate.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val createdAt by viewModel.createdAt.collectAsStateWithLifecycle()
    val resolvedOn by viewModel.resolvedOn.collectAsStateWithLifecycle()
    val cancelledOn by viewModel.cancelledOn.collectAsStateWithLifecycle()
    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val isResolveEnabled by viewModel.isResolveEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showResolveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var productToRemove by rememberSaveable { mutableStateOf<Int?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.order_detail_action_failed)

    LaunchedEffect(isError) {
        if (isError) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.resetErrorState()
        }
    }

    /** Leaving without saving: clean up an order created only to host a product. */
    fun navigateBack() {
        viewModel.discardIfAbandoned()
        onNavigateBack(false)
    }

    BackHandler { navigateBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isNewOrder) {
                                R.string.order_detail_title_new
                            } else {
                                R.string.order_detail_title_edit
                            }
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                },
                actions = {
                    // Lifecycle actions only apply to an existing order.
                    if (!viewModel.isNewOrder) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.common_more),
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (isEditable) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.order_detail_cancel_order))
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showCancelConfirmation = true
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.order_detail_delete),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Hidden for terminal orders: nothing left to save or resolve.
            if (isEditable && !isLoading && !loadFailed) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Resolve only makes sense once the order exists
                        if (!viewModel.isNewOrder) {
                            OutlinedButton(
                                onClick = { showResolveConfirmation = true },
                                enabled = isResolveEnabled && !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SageGreenDark
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.order_detail_resolve_short),
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.saveOrder { onNavigateBack(true) } },
                            enabled = isSaveEnabled && !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = Color.White,
                                disabledContainerColor = SageGreenLight,
                                disabledContentColor = TextSecondary
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.order_detail_save),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }

            loadFailed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.order_detail_load_failed),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { viewModel.retry() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.order_detail_retry))
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Status Badge ───────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OrderStatusBadge(status = status)
                }

                // ── Customer Name ──────────────
                OutlinedTextField(
                    value = customerName,
                    onValueChange = viewModel::onCustomerNameChange,
                    label = { Text(stringResource(R.string.order_detail_customer_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditable,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FloralWhite,
                        unfocusedContainerColor = FloralWhite,
                        disabledContainerColor = FloralWhite,
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = SageGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = SageGreen
                    )
                )

                // ── Due Date ───────────────────
                OutlinedTextField(
                    value = dueDate?.format(DATE_FORMAT) ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.order_detail_due_date)) },
                    placeholder = {
                        Text(
                            stringResource(R.string.order_detail_due_date_placeholder),
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = isEditable,
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            enabled = isEditable
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.order_detail_pick_date),
                                tint = SageGreen
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FloralWhite,
                        unfocusedContainerColor = FloralWhite,
                        disabledContainerColor = FloralWhite,
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = SageGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = SageGreen
                    )
                )

                // ── Note ───────────────────────
                OutlinedTextField(
                    value = note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(stringResource(R.string.order_detail_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditable,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FloralWhite,
                        unfocusedContainerColor = FloralWhite,
                        disabledContainerColor = FloralWhite,
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = SageGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = SageGreen
                    )
                )

                // ── Products Section ───────────
                ProductFormDivider(
                    label = stringResource(R.string.order_detail_section_products)
                        .uppercase(Locale.ROOT)
                )

                if (products.isEmpty()) {
                    Text(
                        text = stringResource(R.string.order_detail_no_products),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                } else {
                    products.forEach { product ->
                        OrderProductItem(
                            product = product,
                            imageFile = product.imageList.firstOrNull()
                                ?.fileName
                                ?.let(viewModel::imageFile),
                            canRemove = isEditable,
                            onClick = { onNavigateToEditProduct(product.details.id) },
                            onRemove = { productToRemove = product.details.id }
                        )
                    }

                    // ── Total ──────────────────
                    val total = remember(products) { products.orderTotal() }
                    if (total != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.order_detail_total),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = total.format(),
                                style = MaterialTheme.typography.titleLarge,
                                color = AccentGold
                            )
                        }
                    }
                }

                // ── Add Product ────────────────
                if (isEditable) {
                    OutlinedButton(
                        onClick = {
                            viewModel.prepareForAddProduct { orderId ->
                                onNavigateToAddProduct(orderId)
                            }
                        },
                        enabled = isSaveEnabled && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SageGreenDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.order_detail_add_product))
                    }

                    if (!isSaveEnabled) {
                        Text(
                            text = stringResource(R.string.order_detail_add_product_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Read Only Dates ────────────
                if (!viewModel.isNewOrder) {
                    ProductFormDivider(
                        label = stringResource(R.string.order_detail_section_details)
                            .uppercase(Locale.ROOT)
                    )

                    ProductReadOnlyDateField(
                        label = stringResource(R.string.order_detail_created_at),
                        date = createdAt
                    )

                    when (status) {
                        OrderStatus.RESOLVED -> ProductReadOnlyDateField(
                            label = stringResource(R.string.order_detail_resolved_on),
                            date = resolvedOn
                        )
                        OrderStatus.CANCELLED -> ProductReadOnlyDateField(
                            label = stringResource(R.string.order_detail_cancelled_on),
                            date = cancelledOn
                        )
                        OrderStatus.OPEN -> Unit
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // ── Date Picker ────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: LocalDate.now()).toPickerMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(20.dp),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toLocalDateFromPicker()
                            ?.let(viewModel::onDueDateChange)
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen,
                        contentColor = Color.White,
                        disabledContainerColor = SageGreenLight,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.common_ok),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = SageGreen,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = SageGreen,
                    todayContentColor = SageGreen,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor = TextSecondary.copy(alpha = 0.3f),
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    navigationContentColor = SageGreenDark,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedYearContainerColor = SageGreen,
                    selectedYearContentColor = Color.White,
                    currentYearContentColor = SageGreen
                )
            )
        }
    }

    // ── Confirmations ──────────────────────────
    if (showResolveConfirmation) {
        OrderConfirmationDialog(
            title = stringResource(R.string.order_detail_resolve_confirm_title),
            message = pluralStringResource(
                R.plurals.order_detail_resolve_confirm_message,
                products.size,
                products.size
            ),
            confirmLabel = stringResource(R.string.order_detail_resolve),
            onConfirm = {
                showResolveConfirmation = false
                viewModel.resolveOrder { onNavigateBack(true) }
            },
            onDismiss = { showResolveConfirmation = false }
        )
    }

    if (showCancelConfirmation) {
        OrderConfirmationDialog(
            title = stringResource(R.string.order_detail_cancel_confirm_title),
            message = stringResource(R.string.order_detail_cancel_confirm_message),
            confirmLabel = stringResource(R.string.order_detail_cancel_order),
            isDestructive = true,
            onConfirm = {
                showCancelConfirmation = false
                viewModel.cancelOrder { onNavigateBack(true) }
            },
            onDismiss = { showCancelConfirmation = false }
        )
    }

    if (showDeleteConfirmation) {
        OrderConfirmationDialog(
            title = stringResource(R.string.order_detail_delete_confirm_title),
            message = stringResource(R.string.order_detail_delete_confirm_message),
            confirmLabel = stringResource(R.string.order_detail_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteOrder { onNavigateBack(false) }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    productToRemove?.let { productId ->
        OrderConfirmationDialog(
            title = stringResource(R.string.order_detail_remove_product_confirm_title),
            message = stringResource(R.string.order_detail_remove_product_confirm_message),
            confirmLabel = stringResource(R.string.order_detail_remove_product),
            onConfirm = {
                productToRemove = null
                viewModel.removeProductFromOrder(productId)
            },
            onDismiss = { productToRemove = null }
        )
    }
}

// ── Confirmation Dialog ────────────────────────────────────────────────
@Composable
private fun OrderConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        SageGreenDark
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}