package app.youtop.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.youtop.data.Spot
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.max

private val Palettes = listOf(
    listOf(Color(0xFF1B1F4B), Color(0xFF5B3DF5), Color(0xFF0FB5BA)),
    listOf(Color(0xFF2A0F3D), Color(0xFFB5179E), Color(0xFFF77F00)),
    listOf(Color(0xFF05263B), Color(0xFF0077B6), Color(0xFF90E0EF)),
    listOf(Color(0xFF10241A), Color(0xFF2D6A4F), Color(0xFFB7E4C7)),
)

/**
 * Fullscreen lesson image with a "camera" that glides to the region being
 * narrated: the image zooms towards [spot], everything else dims, and a ring
 * marks the exact place. With `spot == null` the camera pulls back out.
 */
@Composable
fun SpotlightImage(model: Any?, spot: Spot?, seed: Int, modifier: Modifier = Modifier) {
    var intrinsic by remember(model) { mutableStateOf(Size.Unspecified) }
    // Keep the last spot so the highlight fades out where it was instead of jumping.
    var last by remember { mutableStateOf(spot ?: Spot(0.5f, 0.5f, 0.3f)) }
    if (spot != null) last = spot

    BoxWithConstraints(modifier.clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // Where the image lands on screen under ContentScale.Crop.
        val iw = if (intrinsic.isSpecified && intrinsic.width > 0) intrinsic.width else w
        val ih = if (intrinsic.isSpecified && intrinsic.height > 0) intrinsic.height else h
        val fit = max(w / iw, h / ih)
        val drawnW = iw * fit
        val drawnH = ih * fit
        val left = (w - drawnW) / 2f
        val top = (h - drawnH) / 2f

        val targetX = left + last.x * drawnW
        val targetY = top + last.y * drawnH
        val targetR = last.r * drawnW
        val targetZoom = if (spot != null) (0.26f * w / targetR).coerceIn(1.1f, 1.5f) else 1f
        // Aim a little above the middle: captions sit at the bottom.
        val targetTx = if (spot != null) (w * 0.5f - targetX * targetZoom).coerceIn(w - w * targetZoom, 0f) else 0f
        val targetTy = if (spot != null) (h * 0.42f - targetY * targetZoom).coerceIn(h - h * targetZoom, 0f) else 0f

        val glide = tween<Float>(durationMillis = 750, easing = FastOutSlowInEasing)
        val zoom by animateFloatAsState(targetZoom, glide, label = "zoom")
        val tx by animateFloatAsState(targetTx, glide, label = "tx")
        val ty by animateFloatAsState(targetTy, glide, label = "ty")
        val cx by animateFloatAsState(targetX, glide, label = "cx")
        val cy by animateFloatAsState(targetY, glide, label = "cy")
        val radius by animateFloatAsState(targetR, glide, label = "r")
        val dim by animateFloatAsState(if (spot != null) 1f else 0f, tween(500), label = "dim")

        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "pulse",
        )

        val palette = Palettes[abs(seed) % Palettes.size]
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = tx
                    translationY = ty
                }
                .background(Brush.verticalGradient(palette))
        ) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { intrinsic = it.painter.intrinsicSize },
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                if (dim <= 0.01f) return@Canvas
                val center = Offset(cx, cy)
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f * dim),
                        center = center,
                        radius = radius * 2f,
                    )
                )
                val stroke = 2.5.dp.toPx() / zoom
                drawCircle(Color.White.copy(alpha = 0.9f * dim), radius, center, style = Stroke(stroke))
                drawCircle(
                    Color.White.copy(alpha = 0.45f * (1f - pulse) * dim),
                    radius * (1f + 0.22f * pulse), center, style = Stroke(stroke),
                )
            }
        }
    }
}
