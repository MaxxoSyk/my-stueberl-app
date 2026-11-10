package de.landstueberl.mystueberlapp.view.event

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.format
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlConfirmationDialog
import de.landstueberl.mystueberlapp.viewmodel.event.EventDetailUiState
import de.landstueberl.mystueberlapp.viewmodel.event.EventDetailViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val CREATED_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (eventId: Int) -> Unit,
    eventSaved: Boolean = false,
    onEventSavedConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showStartConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

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
            if (event?.status == EventStatus.PLANNED) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
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

            is EventDetailUiState.Loaded -> EventDetailContent(
                event = state.event,
                modifier = Modifier.padding(padding)
            )
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
}

// ── Content ────────────────────────────────────────────────────────────
@Composable
private fun EventDetailContent(
    event: EventDetail,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            EventStatusBadge(status = event.status)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )

                HorizontalDivider(color = colorScheme.primary.copy(alpha = 0.3f))

                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.event_form_start_date),
                    value = eventDateText(event)
                )

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

        if (event.status == EventStatus.PLANNED) {
            Text(
                text = stringResource(R.string.event_detail_planned_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = stringResource(
                R.string.event_detail_created_at,
                event.createdAt.format(CREATED_FORMAT)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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