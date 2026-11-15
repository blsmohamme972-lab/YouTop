package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Building blocks shared by the library's pages.

private val CardShape = RoundedCornerShape(20.dp)

/** A raised panel that groups related things. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    title: String? = null,
    spacing: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(Surface1)
            .border(1.dp, Color.White.copy(alpha = 0.06f), CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        if (title != null) SectionTitle(title)
        content()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = modifier,
)

@Composable
fun Label(text: String) = Text(text, color = Faint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) OnAccent else Color.White,
        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) Accent else Color.White.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** A switch with what it does and, underneath, a line saying when to use it. */
@Composable
fun Toggle(label: String, checked: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = Color.White, fontSize = 15.sp)
            if (hint != null) Text(hint, color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun Stepper(
    label: String,
    value: Float,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    percent: Boolean = false,
    onChange: (Float) -> Unit,
) {
    fun snap(v: Float) = ((v / step).roundToInt() * step).coerceIn(range.start, range.endInclusive)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        RoundIcon(Icons.Rounded.Remove) { onChange(snap(value - step)) }
        Text(
            if (percent) "${(value * 100).roundToInt()}%" else if (value % 1f == 0f) "${value.toInt()}" else "%.1f".format(value),
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.width(58.dp),
        )
        RoundIcon(Icons.Rounded.Add) { onChange(snap(value + step)) }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

/** A list of minutes typed as "1 10 60". */
@Composable
fun Steps(label: String, steps: List<Int>, onChange: (List<Int>) -> Unit) {
    var text by remember(steps) { mutableStateOf(steps.joinToString(" ")) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { typed ->
                text = typed.filter { it.isDigit() || it == ' ' }
                val parsed = text.split(' ').mapNotNull { it.toIntOrNull() }.filter { it in 1..10_080 }
                if (parsed != steps && !text.endsWith(' ')) onChange(parsed)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** A thin bar: how much of something is done. [second] is drawn over [first] (e.g. learned over started). */
@Composable
fun ProgressLine(first: Float, modifier: Modifier = Modifier, second: Float = 0f, height: Dp = 4.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) {
        Box(Modifier.fillMaxWidth(first.coerceIn(0f, 1f)).height(height).clip(CircleShape).background(Accent))
        Box(Modifier.fillMaxWidth(second.coerceIn(0f, 1f)).height(height).clip(CircleShape).background(Good))
    }
}

/** What is shown where a list would be, when there is nothing to list. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(30.dp))
        }
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = Faint, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** The main action of a page: amber, full width. */
@Composable
fun PrimaryButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier.clip(CircleShape).background(if (enabled) Accent else Color.White.copy(alpha = 0.16f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        val ink = if (enabled) OnAccent else Color.White.copy(alpha = 0.6f)
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp)); Box(Modifier.width(8.dp)) }
        Text(text, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A quieter action. */
@Composable
fun SecondaryButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.09f)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)); Box(Modifier.width(8.dp)) }
        Text(text, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A number in the phone's own digits, so it matches the numbers inside translated sentences. */
fun num(n: Int): String = String.format(java.util.Locale.getDefault(), "%d", n)

/** "done / total", kept in that order in right-to-left text too. */
fun ratio(done: Int, total: Int): String = "\u2066${num(done)} / ${num(total)}\u2069"
