package de.landstueberl.mystueberlapp.view.event

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.view.common.StueberlDatePickerDialog
import de.landstueberl.mystueberlapp.viewmodel.event.EventFormViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private const val MAX_LOCATION_SUGGESTIONS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    viewModel: EventFormViewModel,
    onNavigateBack: (eventSaved: Boolean) -> Unit
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val eventDate by viewModel.eventDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val standFee by viewModel.standFee.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val locationSuggestions by viewModel.locationSuggestions.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.event_form_save_failed)

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
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isNewEvent) {
                                R.string.event_form_title_new
                            } else {
                                R.string.event_form_title_edit
                            }
                        ),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                colors = StueberlDefaults.topAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            // ── Loading ────────────────────────
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            // ── Load Failed ────────────────────
            loadFailed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(R.string.event_form_load_failed),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { viewModel.retry() },
                    shape = RoundedCornerShape(10.dp),
                    colors = StueberlDefaults.primaryButtonColors()
                ) {
                    Text(stringResource(R.string.event_form_retry))
                }
            }

            // ── Form ───────────────────────────
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.event_form_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = StueberlDefaults.textFieldColors()
                )

                // Location + suggestions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = viewModel::onLocationChange,
                        label = { Text(stringResource(R.string.event_form_location)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = StueberlDefaults.textFieldColors()
                    )

                    LocationSuggestions(
                        query = location,
                        suggestions = locationSuggestions,
                        onSelect = viewModel::onLocationChange
                    )
                }

                // Dates
                EventDateField(
                    label = stringResource(R.string.event_form_start_date),
                    date = eventDate,
                    onClick = { showStartPicker = true }
                )

                EventDateField(
                    label = stringResource(R.string.event_form_end_date),
                    date = endDate,
                    supportingText = stringResource(R.string.event_form_end_date_hint),
                    enabled = eventDate != null,
                    onClick = { showEndPicker = true },
                    onClear = viewModel::clearEndDate
                )

                // Stand fee
                OutlinedTextField(
                    value = standFee,
                    onValueChange = viewModel::onStandFeeChange,
                    label = { Text(stringResource(R.string.event_form_stand_fee)) },
                    supportingText = { Text(stringResource(R.string.event_form_stand_fee_hint)) },
                    suffix = {
                        Text(
                            text = PriceInput.CURRENCY.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = StueberlDefaults.textFieldColors()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(stringResource(R.string.event_form_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = StueberlDefaults.textFieldColors()
                )

                // Save
                Button(
                    onClick = { viewModel.saveEvent { onNavigateBack(true) } },
                    enabled = isSaveEnabled && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = StueberlDefaults.primaryButtonColors()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.event_form_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (!isSaveEnabled) {
                    Text(
                        text = stringResource(R.string.event_form_save_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ── Date Pickers ───────────────────────────
    if (showStartPicker) {
        StueberlDatePickerDialog(
            initialDate = eventDate,
            onConfirm = { date ->
                viewModel.onEventDateChange(date)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        // The field is disabled without a start date; the check guards a
        // picker restored after process death.
        eventDate?.let { start ->
            StueberlDatePickerDialog(
                initialDate = endDate ?: start,
                minDate = start,
                onConfirm = { date ->
                    viewModel.onEndDateChange(date)
                    showEndPicker = false
                },
                onDismiss = { showEndPicker = false }
            )
        }
    }
}

// ── Date Field ─────────────────────────────────────────────────────────
/**
 * Read-only field that opens a picker when tapped anywhere. With [onClear],
 * a set date shows a clear icon instead of the calendar icon.
 */
@Composable
private fun EventDateField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClear: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)

    // A read-only field has no onClick; its press interactions stand in for one.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) currentOnClick()
        }
    }

    OutlinedTextField(
        value = date?.format(DATE_FORMAT).orEmpty(),
        onValueChange = { },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            if (onClear != null && date != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.event_form_clear_end_date)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        colors = StueberlDefaults.textFieldColors()
    )
}

// ── Location Suggestions ───────────────────────────────────────────────
/**
 * Previously used locations. With an empty field the most used ones are
 * offered; while typing they're filtered, and an exact match is skipped.
 */
@Composable
private fun LocationSuggestions(
    query: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val matches = remember(query, suggestions) {
        val q = query.trim()
        suggestions
            .asSequence()
            .filter {
                q.isEmpty() ||
                        (it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true))
            }
            .take(MAX_LOCATION_SUGGESTIONS)
            .toList()
    }

    if (matches.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        matches.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSelect(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = null
            )
        }
    }
}