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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.youtop.data.Spot
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.min

private val Palettes = listOf(
    listOf(Color(0xFF1B1F4B), Color(0xFF5B3DF5), Color(0xFF0FB5BA)),
    listOf(Color(0xFF2A0F3D), Color(0xFFB5179E), Color(0xFFF77F00)),
    listOf(Color(0xFF05263B), Color(0xFF0077B6), Color(0xFF90E0EF)),
    listOf(Color(0xFF10241A), Color(0xFF2D6A4F), Color(0xFFB7E4C7)),
)

/** Where the picture sits between the top (-1) and the bottom (1): a little high, so the caption has room under it. */
private const val PictureBias = -0.24f

/**
 * Lesson image with a spotlight on the region being narrated. The whole picture is always
 * shown, never zoomed or cropped (the owner asked for that), so a spot is always on screen
 * exactly where the content says it is. Everything outside the spot dims and a ring marks it.
 * With `spot == null` the light comes back up. A blurred copy fills the screen behind.
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

        // Where the image lands on screen under ContentScale.Fit.
        val iw = if (intrinsic.isSpecified && intrinsic.width > 0) intrinsic.width else w
        val ih = if (intrinsic.isSpecified && intrinsic.height > 0) intrinsic.height else h
        val fit = min(w / iw, h / ih)
        val drawnW = iw * fit
        val drawnH = ih * fit
        val left = (w - drawnW) / 2f
        val top = (h - drawnH) * (1f + PictureBias) / 2f

        val glide = tween<Float>(durationMillis = 450, easing = FastOutSlowInEasing)
        val cx by animateFloatAsState(left + last.x * drawnW, glide, label = "cx")
        val cy by animateFloatAsState(top + last.y * drawnH, glide, label = "cy")
        val radius by animateFloatAsState(last.r * drawnW, glide, label = "r")
        val dim by animateFloatAsState(if (spot != null) 1f else 0f, tween(350), label = "dim")

        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "pulse",
        )

        val palette = Palettes[abs(seed) % Palettes.size]
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(palette))) {
            if (model != null) {
                // Backdrop for the bands above and below the picture.
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(28.dp),
                )
                Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.66f)))
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = BiasAlignment(0f, PictureBias),
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { intrinsic = it.painter.intrinsicSize },
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                if (dim <= 0.01f) return@Canvas
                val center = Offset(cx, cy)
                // Fully lit inside the ring, soft edge just outside it, dim beyond.
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f * dim),
                        center = center,
                        radius = radius * 1.4f,
                    )
                )
                val stroke = 2.5.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.9f * dim), radius, center, style = Stroke(stroke))
                drawCircle(
                    Color.White.copy(alpha = 0.45f * (1f - pulse) * dim),
                    radius * (1f + 0.22f * pulse), center, style = Stroke(stroke),
                )
            }
        }
    }
}
