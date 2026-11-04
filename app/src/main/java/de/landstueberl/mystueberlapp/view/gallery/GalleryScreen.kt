package de.landstueberl.mystueberlapp.view.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.landstueberl.mystueberlapp.R
import de.landstueberl.mystueberlapp.ui.theme.SageGreen
import de.landstueberl.mystueberlapp.ui.theme.SageGreenLight
import de.landstueberl.mystueberlapp.ui.theme.TextSecondary
import de.landstueberl.mystueberlapp.view.product.ProductImagePreviewDialog
import de.landstueberl.mystueberlapp.viewmodel.gallery.GalleryViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SECTION_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

/** Minimum tile width; the grid fits as many columns as the screen allows. */
private val MIN_TILE_WIDTH = 110.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (Int) -> Unit
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
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
                title = {
                    Text(
                        text = stringResource(R.string.gallery_screen_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreen,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val loaded = sections

        when {
            loaded == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SageGreen)
            }

            loaded.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.gallery_screen_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = MIN_TILE_WIDTH),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                loaded.forEach { section ->
                    item(
                        key = "header_${section.date}",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Text(
                            text = section.date.format(SECTION_DATE_FORMAT),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(
                        items = section.entries,
                        key = { it.fileName }
                    ) { entry ->
                        GalleryTileItem(
                            imageFile = remember(entry.fileName) {
                                viewModel.imageFile(entry.fileName)
                            },
                            onClick = { previewFileName = entry.fileName },
                            onLongClick = { onNavigateToProduct(entry.productId) }
                        )
                    }
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

// ── Tile ───────────────────────────────────────────────────────────────
@Composable
private fun GalleryTileItem(
    imageFile: java.io.File,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(SageGreenLight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}