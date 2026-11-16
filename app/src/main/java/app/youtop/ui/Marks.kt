package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.youtop.R
import app.youtop.data.Mark
import app.youtop.data.Spot
import kotlin.math.roundToInt

/**
 * The colour of each numbered circle, by its number. The table row of a circle carries the same
 * one. (tools/content/batch.py draws its check pictures with the same list.)
 */
val MarkColors = listOf(
    Color(0xFFFFD23F), Color(0xFF4CC9F0), Color(0xFFFF6B6B), Color(0xFF7AE582), Color(0xFFC77DFF),
    Color(0xFFFF9F1C), Color(0xFF2EC4B6), Color(0xFFF472B6), Color(0xFFB8F2E6), Color(0xFFE0E0E0),
)

fun markColor(number: Int) = MarkColors[(number - 1).mod(MarkColors.size)]

/** A circle to draw on the picture with its number. [pulse] draws attention to it (the answer's circle). */
data class NumberedSpot(val number: Int, val spot: Spot, val pulse: Boolean = false)

/** The lesson's marks with these [numbers], as circles to draw; numbers that do not exist are left out. */
fun List<Mark>.numbered(numbers: Iterable<Int>, pulse: Set<Int> = emptySet()): List<NumberedSpot> =
    numbers.distinct().mapNotNull { n -> getOrNull(n - 1)?.spot?.let { NumberedSpot(n, it, n in pulse) } }

/** The number of a circle, as it sits on the picture and in the table. */
@Composable
fun MarkBadge(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(20.dp).clip(CircleShape).background(markColor(number)).border(1.dp, Color.Black.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = Ink, style = TextStyle(fontFamily = Ui, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * "All circles": a small translucent table of every numbered circle — its number in its colour,
 * what is in the picture, and what that stands for. Always English and left to right, whatever
 * the phone's language. It floats over the lesson and is dragged with a finger when it covers
 * something; a tap on a row singles out that circle.
 */
@Composable
fun MarksTable(
    marks: List<Mark>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onClose: () -> Unit,
    /** Where the table first appears, as a share of the free height (0 = top, 1 = bottom). */
    startBias: Float,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val edge = with(density) { 8.dp.toPx() }
            val top = with(density) { 36.dp.toPx() }
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            var size by remember { mutableStateOf(IntSize.Zero) }
            // Null until the learner has moved it: until then it sits where [startBias] puts it.
            var moved by remember { mutableStateOf<Offset?>(null) }
            fun clamp(o: Offset) = Offset(
                o.x.coerceIn(edge, (w - size.width - edge).coerceAtLeast(edge)),
                o.y.coerceIn(top, (h - size.height - edge).coerceAtLeast(top)),
            )
            val start = clamp(Offset((w - size.width) / 2f, top + (h - size.height - top - edge) * startBias))
            val at = moved?.let(::clamp) ?: start
            val closeLabel = stringResource(R.string.close)

            Column(
                Modifier
                    .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
                    .alpha(if (size == IntSize.Zero) 0f else 1f)
                    .widthIn(max = 372.dp)
                    .onSizeChanged { size = it }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Ink.copy(alpha = 0.66f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            moved = clamp((moved ?: start) + drag)
                        }
                    }
                    .semantics { contentDescription = "All circles table" }
                    .padding(start = 10.dp, end = 6.dp, bottom = 8.dp),
            ) {
                // The grip says "drag me"; the cross closes.
                Box(Modifier.fillMaxWidth().height(26.dp)) {
                    Box(Modifier.align(Alignment.Center).width(34.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.38f)))
                    Box(
                        Modifier.align(Alignment.CenterEnd).size(26.dp).clip(CircleShape).clickable(onClickLabel = closeLabel, onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, closeLabel, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
                val head = TextStyle(fontFamily = Ui, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, textDirection = TextDirection.Ltr)
                Row(Modifier.padding(end = 6.dp, bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("#", color = Faint, style = head, modifier = Modifier.width(20.dp).padding(start = 6.dp))
                    Text("IN THE PICTURE", color = Faint, style = head, modifier = Modifier.weight(0.40f))
                    Text("STANDS FOR", color = Faint, style = head, modifier = Modifier.weight(0.60f))
                }
                marks.forEachIndexed { i, mark ->
                    val number = i + 1
                    val colour = markColor(number)
                    val on = selected == number
                    Box(Modifier.fillMaxWidth().padding(end = 4.dp).height(1.dp).background(Color.White.copy(alpha = 0.09f)))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) colour.copy(alpha = 0.20f) else Color.Transparent)
                            .clickable { onSelect(if (on) null else number) }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MarkBadge(number)
                        Text(
                            mark.what, color = colour,
                            style = TextStyle(fontFamily = Ui, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, textDirection = TextDirection.Ltr),
                            modifier = Modifier.weight(0.40f),
                        )
                        Text(
                            mark.means, color = Color.White.copy(alpha = 0.94f),
                            style = TextStyle(fontFamily = Ui, fontSize = 11.5.sp, lineHeight = 15.sp, textDirection = TextDirection.Ltr),
                            modifier = Modifier.weight(0.60f),
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}
