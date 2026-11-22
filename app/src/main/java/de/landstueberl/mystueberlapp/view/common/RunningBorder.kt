package de.landstueberl.mystueberlapp.view.common

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The tail is drawn as this many short pieces, fading towards its end. */
private const val TAIL_SLICES = 16

/**
 * Bold frame with a lighter highlight running around it.
 *
 * The highlight moves along the frame by distance, so its speed is the same on
 * every edge and around the corners. With system animations turned off, only
 * the solid frame is drawn. Only the draw phase reads the animated position,
 * so the content isn't recomposed each frame.
 *
 * @param cornerRadius must match the shape of the element it's applied to
 * @param highlightFraction share of the frame's length the highlight covers
 */
@Composable
fun Modifier.runningBorder(
    color: Color,
    highlight: Color,
    width: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    durationMillis: Int = 5_000,
    highlightFraction: Float = 0.2f
): Modifier {
    // Read once when the tile appears; a settings change applies the next time.
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }

    val progress: State<Float>? = if (animationsEnabled) {
        rememberInfiniteTransition(label = "runningBorder").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
            label = "progress"
        )
    } else {
        null
    }

    return drawWithCache {
        val strokePx = width.toPx()
        val half = strokePx / 2

        // The frame's centre line; the stroke fills the ring around it.
        // Clockwise, so the highlight runs clockwise (the default is counter-clockwise).
        val outline = Path().apply {
            addRoundRect(
                roundRect = RoundRect(
                    left = half,
                    top = half,
                    right = size.width - half,
                    bottom = size.height - half,
                    cornerRadius = CornerRadius((cornerRadius.toPx() - half).coerceAtLeast(0f))
                ),
                direction = Path.Direction.Clockwise
            )
        }
        val measure = PathMeasure().apply { setPath(outline, forceClosed = true) }
        val length = measure.length
        val tailLength = length * highlightFraction.coerceIn(0f, 1f)
        val sliceLength = tailLength / TAIL_SLICES
        val slices = List(TAIL_SLICES) { Path() }
        val stroke = Stroke(width = strokePx)

        onDrawWithContent {
            drawContent()
            drawPath(path = outline, color = color, style = stroke)

            // Static frame when animations are off.
            val position = progress?.value ?: return@onDrawWithContent
            val head = position * length

            slices.forEachIndexed { index, slice ->
                slice.reset()
                val from = head - tailLength + index * sliceLength
                measure.appendSegment(from, from + sliceLength, length, slice)
                // Faint at the end of the tail, full strength at the head.
                drawPath(
                    path = slice,
                    color = highlight.copy(alpha = highlight.alpha * (index + 1f) / TAIL_SLICES),
                    style = stroke
                )
            }
        }
    }
}

/**
 * Adds the part of the frame between [from] and [to] (distances along it) to
 * [dest]. Handles the tail reaching back past the start of the frame.
 */
private fun PathMeasure.appendSegment(from: Float, to: Float, length: Float, dest: Path) {
    var start = from
    var end = to
    if (end <= 0f) {
        start += length
        end += length
    }
    if (start < 0f) {
        getSegment(start + length, length, dest, startWithMoveTo = true)
        getSegment(0f, end, dest, startWithMoveTo = true)
    } else {
        getSegment(start, end, dest, startWithMoveTo = true)
    }
}