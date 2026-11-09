package de.landstueberl.mystueberlapp.view.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.data.EventSections
import de.landstueberl.mystueberlapp.data.EventWithProductCount
import de.landstueberl.mystueberlapp.ui.theme.StueberlDefaults
import de.landstueberl.mystueberlapp.viewmodel.event.EventsViewModel
import java.util.Locale

/** FAB (56 dp) plus its margin and some air, so the last card stays tappable. */
private val FAB_CLEARANCE = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEvent: (Int) -> Unit,
    onNavigateToNewEvent: () -> Unit,
    eventSaved: Boolean = false,
    onEventSavedConsumed: () -> Unit = {}
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.events_screen_success_save)

    LaunchedEffect(eventSaved) {
        if (eventSaved) {
            snackbarHostState.showSnackbar(savedMessage)
            onEventSavedConsumed()
        }
    }

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
                        text = stringResource(R.string.events_screen_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                colors = StueberlDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewEvent,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.events_screen_add)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val current = sections
        when {
            // Still loading: render nothing rather than flash the empty state.
            current == null -> Box(modifier = Modifier.fillMaxSize().padding(padding))

            current.isEmpty -> EmptyEvents(modifier = Modifier.padding(padding))

            else -> EventList(
                sections = current,
                onEventClick = onNavigateToEvent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ── List ───────────────────────────────────────────────────────────────
@Composable
private fun EventList(
    sections: EventSections,
    onEventClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = FAB_CLEARANCE
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Upcoming is always shown, so "Past" never looks like the only section
        // when there's simply nothing planned right now.
        item(key = "header_upcoming", contentType = "header") {
            EventSectionHeader(stringResource(R.string.events_screen_section_upcoming))
        }
        if (sections.upcoming.isEmpty()) {
            item(key = "no_upcoming", contentType = "hint") {
                Text(
                    text = stringResource(R.string.events_screen_no_upcoming),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            eventItems(sections.upcoming, onEventClick)
        }

        if (sections.past.isNotEmpty()) {
            item(key = "header_past", contentType = "header") {
                EventSectionHeader(
                    text = stringResource(R.string.events_screen_section_past),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            eventItems(sections.past, onEventClick)
        }
    }
}

/** An event sits in exactly one section, so its id is a unique key. */
private fun LazyListScope.eventItems(
    events: List<EventWithProductCount>,
    onEventClick: (Int) -> Unit
) {
    items(
        items = events,
        key = { it.event.id },
        contentType = { "event" }
    ) { item ->
        EventDisplayItem(
            item = item,
            onClick = { onEventClick(item.event.id) }
        )
    }
}

@Composable
private fun EventSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

// ── Empty State ────────────────────────────────────────────────────────
@Composable
private fun EmptyEvents(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.events_screen_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}