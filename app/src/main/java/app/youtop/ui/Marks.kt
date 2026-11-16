package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * "All circles": the table of every numbered circle — its number in its colour, what is in the
 * picture, and what that stands for. Always English and left to right, whatever the phone's
 * language. It is fixed under the picture (which moves up to make room, see [DockShare]) and its
 * rows scroll; a tap on a row singles out that circle.
 */
@Composable
fun MarksTable(
    marks: List<Mark>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        DockPanel(modifier.semantics { contentDescription = "All circles table" }) {
            val closeLabel = stringResource(R.string.close)
            val head = TextStyle(fontFamily = Ui, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, textDirection = TextDirection.Ltr)
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("#", color = Faint, style = head, modifier = Modifier.width(22.dp).padding(start = 7.dp))
                Text("IN THE PICTURE", color = Faint, style = head, modifier = Modifier.weight(0.40f))
                Text("STANDS FOR", color = Faint, style = head, modifier = Modifier.weight(0.60f))
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClickLabel = closeLabel, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, closeLabel, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                marks.forEachIndexed { i, mark ->
                    val number = i + 1
                    val colour = markColor(number)
                    val on = selected == number
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.09f)))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) colour.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onSelect(if (on) null else number) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MarkBadge(number, Modifier.padding(start = 1.dp))
                        Text(
                            mark.what, color = colour,
                            style = TextStyle(fontFamily = Ui, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, textDirection = TextDirection.Ltr),
                            modifier = Modifier.weight(0.40f),
                        )
                        Text(
                            mark.means, color = Color.White.copy(alpha = 0.94f),
                            style = TextStyle(fontFamily = Ui, fontSize = 13.sp, lineHeight = 18.sp, textDirection = TextDirection.Ltr),
                            modifier = Modifier.weight(0.60f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The fixed panel under a docked picture: the bottom [DockShare] of the screen, clear of the
 * phone's own buttons. What does not fit scrolls inside it; nothing of it lies over the picture.
 */
@Composable
fun DockPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(DockShare)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Surface1)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                // Touches on the panel are the panel's: they never reach the picture behind it.
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding(),
            content = content,
        )
    }
}
