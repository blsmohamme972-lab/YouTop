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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** The share of the screen's height a docked panel takes; the picture sits whole in the rest. */
const val DockShare = 0.43f

/**
 * Lesson image with a spotlight on the region being narrated. The whole picture is always
 * shown, never zoomed or cropped (the owner asked for that), so a spot is always on screen
 * exactly where the content says it is. Everything outside the spot dims and a ring marks it.
 * With `spot == null` the light comes back up. A blurred copy fills the screen behind.
 */
@Composable
fun SpotlightImage(
    model: Any?,
    spot: Spot?,
    seed: Int,
    modifier: Modifier = Modifier,
    /** Numbered circles to show at once, each in its own colour (the question's circles, or all of them). */
    marks: List<NumberedSpot> = emptyList(),
    /** The number of the circle singled out in the table; the others fade. */
    focus: Int? = null,
    /**
     * True while a panel (the circles' table, or the answer and its explanation) holds the bottom
     * [DockShare] of the screen: the picture, still whole, moves up and shrinks to sit above it, so
     * nothing covers it.
     */
    docked: Boolean = false,
) {
    var intrinsic by remember(model) { mutableStateOf(Size.Unspecified) }
    // Keep the last spot so the highlight fades out where it was instead of jumping.
    var last by remember { mutableStateOf(spot ?: Spot(0.5f, 0.5f, 0.3f)) }
    if (spot != null) last = spot
    // Likewise the circles: they fade out as they were.
    var lastMarks by remember { mutableStateOf(marks) }
    if (marks.isNotEmpty()) lastMarks = marks
    val marksShown by animateFloatAsState(if (marks.isNotEmpty()) 1f else 0f, tween(260), label = "marks")
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier.clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // Where the image lands on screen under ContentScale.Fit.
        val iw = if (intrinsic.isSpecified && intrinsic.width > 0) intrinsic.width else w
        val ih = if (intrinsic.isSpecified && intrinsic.height > 0) intrinsic.height else h
        val dock by animateFloatAsState(if (docked) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "dock")
        val padTop = dock * (WindowInsets.statusBars.getTop(LocalDensity.current) + 6.dp.value * LocalDensity.current.density)
        val padBottom = dock * (h * DockShare + 8.dp.value * LocalDensity.current.density)
        val areaH = (h - padTop - padBottom).coerceAtLeast(1f)
        val bias = PictureBias * (1f - dock)
        val fit = min(w / iw, areaH / ih)
        val drawnW = iw * fit
        val drawnH = ih * fit
        val left = (w - drawnW) / 2f
        val top = padTop + (areaH - drawnH) * (1f + bias) / 2f

        val glide = tween<Float>(durationMillis = 450, easing = FastOutSlowInEasing)
        val cx by animateFloatAsState(left + last.x * drawnW, glide, label = "cx")
        val cy by animateFloatAsState(top + last.y * drawnH, glide, label = "cy")
        val radius by animateFloatAsState(last.r.coerceIn(0.03f, 0.6f) * drawnW, glide, label = "r")
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
                    alignment = BiasAlignment(0f, bias),
                    modifier = Modifier.fillMaxSize().padding(
                        top = with(LocalDensity.current) { padTop.toDp() },
                        bottom = with(LocalDensity.current) { padBottom.toDp() },
                    ),
                    onSuccess = { intrinsic = it.painter.intrinsicSize },
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                if (marksShown <= 0.01f) return@Canvas
                val ring = 3.dp.toPx()
                val badge = 11.dp.toPx()
                for (mark in lastMarks) {
                    val colour = markColor(mark.number)
                    val a = marksShown * (if (focus == null || focus == mark.number) 1f else 0.28f)
                    val c = Offset(left + mark.spot.x * drawnW, top + mark.spot.y * drawnH)
                    val r = mark.spot.r.coerceIn(0.03f, 0.6f) * drawnW
                    drawCircle(Color.Black.copy(alpha = 0.45f * a), r, c, style = Stroke(ring + 2.dp.toPx()))
                    drawCircle(colour.copy(alpha = a), r, c, style = Stroke(ring))
                    if (mark.pulse || focus == mark.number) {
                        drawCircle(colour.copy(alpha = 0.6f * (1f - pulse) * a), r * (1f + 0.2f * pulse), c, style = Stroke(ring))
                    }
                    // The number rides on the ring, upper left, so it does not hide what is circled.
                    val b = Offset(
                        (c.x - r * 0.707f).coerceIn(badge + 2f, (size.width - badge - 2f).coerceAtLeast(badge + 2f)),
                        (c.y - r * 0.707f).coerceIn(badge + 2f, (size.height - badge - 2f).coerceAtLeast(badge + 2f)),
                    )
                    drawCircle(Color.Black.copy(alpha = 0.55f * a), badge + 1.5.dp.toPx(), b)
                    drawCircle(colour.copy(alpha = a), badge, b)
                    val label = measurer.measure("${mark.number}", TextStyle(fontFamily = Ui, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = a)))
                    drawText(label, topLeft = Offset(b.x - label.size.width / 2f, b.y - label.size.height / 2f))
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                if (dim <= 0.01f) return@Canvas
                val center = Offset(cx, cy)
                // Fully lit inside the ring, soft edge just outside it, dim beyond.
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.52f * dim),
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
