package app.youtop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import app.youtop.data.MediaCache
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Glass = Color.Black.copy(alpha = 0.30f)
val Accent = Color(0xFFFFD166)
val TextShadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 1f), 6f)

/** Small translucent round button that floats over the image. */
@Composable
fun GlassButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    caption: String? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Glass)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        if (caption != null) {
            Text(
                caption,
                color = Color.White,
                style = TextStyle(fontFamily = Ui, fontSize = 10.sp, fontWeight = FontWeight.Medium, shadow = TextShadow),
                maxLines = 1,
            )
        }
    }
}

/** Thin progress line that can be tapped or dragged to seek. */
@Composable
fun SeekBar(progress: () -> Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    fun fraction(x: Float, width: Int) = (x / width).coerceIn(0f, 1f).let { if (rtl) 1f - it else it }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(rtl) {
                detectTapGestures { seek(fraction(it.x, size.width)) }
            }
            .pointerInput(rtl) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = fraction(it.x, size.width) },
                    onDragEnd = { dragging?.let(seek); dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, _ -> dragging = fraction(change.position.x, size.width) }
            }
    ) {
        val shown = dragging ?: progress()
        val thick = (if (dragging != null) 5.dp else 3.dp).toPx()
        val y = size.height / 2f - thick / 2f
        val filled = size.width * shown
        val corner = CornerRadius(thick / 2f)
        drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(0f, y), Size(size.width, thick), corner)
        drawRoundRect(Color.White, Offset(if (rtl) size.width - filled else 0f, y), Size(filled, thick), corner)
        drawCircle(
            Color.White,
            radius = (if (dragging != null) 8.dp else 5.dp).toPx(),
            center = Offset(if (rtl) size.width - filled else filled, size.height / 2f),
        )
    }
}

/**
 * A picture from the app's own store: shown at once when already downloaded, otherwise downloaded
 * once (see [MediaCache.localImage]) and then shown. Null while it is on its way.
 */
@Composable
fun rememberLocalImage(media: MediaCache, ref: String?): Any? {
    val stored = remember(ref) { media.cachedOrNull(ref) }
    val model by produceState(stored, ref) { value = media.cachedOrNull(ref) ?: media.localImage(ref) }
    return model
}

fun isRtl(lang: String) = lang.startsWith("ar") || lang.startsWith("fa") || lang.startsWith("ur") || lang.startsWith("he")

/** Opens [url] in the phone's browser (or the GitHub app). False when nothing could open it. */
fun openLink(context: android.content.Context, url: String): Boolean = runCatching {
    context.startActivity(
        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}.isSuccess
