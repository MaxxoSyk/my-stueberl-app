package de.landstueberl.mystueberlapp.view.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bold frame with a lighter highlight running around it. Only the draw phase
 * reads the animated angle, so the content isn't recomposed each frame.
 *
 * @param cornerRadius must match the shape of the element it's applied to
 */
@Composable
fun Modifier.runningBorder(
    color: Color,
    highlight: Color,
    width: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    durationMillis: Int = 5_000,
    /** Share of the full lap the highlight's tail covers; smaller = shorter comet. */
    highlightFraction: Float = 0.2f
): Modifier {
    val transition = rememberInfiniteTransition(label = "runningBorder")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "angle"
    )

    return drawWithCache {
        val strokePx = width.toPx()
        val radiusPx = cornerRadius.toPx()

        // The ring the highlight is clipped to: outer rounded rect minus inner one.
        val outer = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
        }
        val inner = Path().apply {
            val innerRadius = (radiusPx - strokePx).coerceAtLeast(0f)
            addRoundRect(
                RoundRect(
                    strokePx, strokePx, size.width - strokePx, size.height - strokePx,
                    CornerRadius(innerRadius)
                )
            )
        }
        val ring = Path().apply { op(outer, inner, PathOperation.Difference) }

        // Comet: transparent most of the way round, a short fading tail and a
        // bright head just before the end of the lap.
        val tailStart = 1f - highlightFraction
        val head = 1f - highlightFraction * 0.15f
        val brush = Brush.sweepGradient(
            0f to Color.Transparent,
            tailStart to Color.Transparent,
            head to highlight,
            1f to Color.Transparent,
            center = Offset(size.width / 2, size.height / 2)
        )

        onDrawWithContent {
            drawContent()

            // Solid base frame, drawn on the centre line of the ring.
            val half = strokePx / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(half, half),
                size = size.copy(width = size.width - strokePx, height = size.height - strokePx),
                cornerRadius = CornerRadius((radiusPx - half).coerceAtLeast(0f)),
                style = Stroke(strokePx)
            )

            // Highlight: rotate a large gradient disc behind the ring clip.
            clipPath(ring) {
                rotate(angle.value) {
                    drawCircle(brush = brush, radius = size.maxDimension)
                }
            }
        }
    }
}