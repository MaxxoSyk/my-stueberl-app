package de.landstueberl.mystueberlapp.view.product

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import de.landstueberl.mystueberlapp.R
import java.io.File

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

@Composable
fun ProductImagePreviewDialog(
    imageFile: File,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(MIN_SCALE) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        /** Keeps the image from being dragged entirely off-screen. */
        fun clampOffset(candidate: Offset, forScale: Float): Offset {
            if (forScale <= MIN_SCALE) return Offset.Zero
            val maxX = containerSize.width * (forScale - 1f) / 2f
            val maxY = containerSize.height * (forScale - 1f) / 2f
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY)
            )
        }

        fun reset() {
            scale = MIN_SCALE
            offset = Offset.Zero
        }

        // Animate only the double-tap jump; pinch updates must stay immediate
        // or the gesture feels laggy.
        var animateChanges by remember { mutableStateOf(false) }
        val renderedScale by animateFloatAsState(targetValue = scale, label = "scale")
        val effectiveScale = if (animateChanges) renderedScale else scale

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .onSizeChanged { containerSize = it }
        ) {
            AsyncImage(
                model = imageFile,
                contentDescription = stringResource(R.string.product_form_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = effectiveScale,
                        scaleY = effectiveScale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    // Double tap: zoom in, or reset if already zoomed
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                animateChanges = true
                                if (scale > MIN_SCALE) {
                                    reset()
                                } else {
                                    scale = DOUBLE_TAP_SCALE
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    // Pinch to zoom, drag to pan
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            animateChanges = false
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scale = newScale
                            offset = clampOffset(
                                candidate = if (newScale <= MIN_SCALE) {
                                    Offset.Zero
                                } else {
                                    offset + pan
                                },
                                forScale = newScale
                            )
                        }
                    }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = Color.White
                )
            }
        }
    }
}