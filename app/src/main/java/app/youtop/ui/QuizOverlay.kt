package app.youtop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.youtop.R
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.youtop.data.MediaCache
import app.youtop.data.correct
import app.youtop.data.multi
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import app.youtop.data.Question
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay


/** Options such as "all of the above" only make sense in their printed order. */
private val Positional = Regex(
    """\b(above|both|all of|none of|neither|[a-e] (and|&|or) [a-e])\b|ما سبق|مما سبق|كلاهما|كل ما ذكر|جميع ما ذكر""",
    RegexOption.IGNORE_CASE,
)

fun optionOrder(question: Question, shuffle: Boolean): List<Int> =
    if (shuffle && question.options.none { Positional.containsMatchIn(it) }) question.options.indices.shuffled()
    else question.options.indices.toList()

/** Where the learner is in a lesson's questions. Lives with the lesson page, so closing the overlay does not lose it. */
class QuizState {
    var index by mutableIntStateOf(0)
    var picked by mutableStateOf<Int?>(null)
    /** The options ticked so far in a question with several right answers. */
    var chosen by mutableStateOf<Set<Int>>(emptySet())
    /** True once a question with several right answers has been checked. */
    var checked by mutableStateOf(false)
    /** True once the question on screen has been answered, by a tap or by "Check". */
    val answered: Boolean get() = picked != null || checked
    /** Clears the answer, for the next question. */
    fun clear() { picked = null; chosen = emptySet(); checked = false }
    /** True once the last question has been answered. */
    var done by mutableStateOf(false)
    /** The shuffled order each question was first shown in, so it does not change when reopened. */
    val orders = HashMap<Int, List<Int>>()
}

/**
 * The lesson's questions, in a fixed panel under the picture (see [DockPanel]): the picture is
 * docked above it, whole, so neither the question nor its options hide the circles that belong
 * to the question. Reports each answer; after the last one the
 * learner moves on when they choose to (nothing moves by itself).
 */
@Composable
fun QuizOverlay(
    questions: List<Question>,
    state: QuizState,
    media: MediaCache,
    shuffle: Boolean,
    speak: Boolean,
    onSay: (String, String) -> Unit,
    onAnswer: (correct: Boolean, tookMs: Long) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onWatch: () -> Unit,
    /** Once the lesson is rated: when it comes back ("in 4 days"), so the scheduling is not a mystery. */
    nextReview: String? = null,
    /** Shows every numbered circle and their table; null when the lesson has none. */
    onAllCircles: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    var index by state::index
    var picked by state::picked
    var shownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // A sync can replace the lesson while its question is open: never index past what is there now.
    val question = questions.getOrNull(index.coerceIn(0, (questions.size - 1).coerceAtLeast(0))) ?: return
    val order = remember(index, question) {
        state.orders[index]?.takeIf { it.size == question.options.size } ?: optionOrder(question, shuffle).also { state.orders[index] = it }
    }
    val last = index >= questions.lastIndex
    val stemScroll = rememberScrollState()
    val scroll = rememberScrollState()
    val explainScroll = rememberScrollState()
    val direction = if (isRtl(question.lang)) TextDirection.Rtl else TextDirection.Ltr
    val screen = LocalConfiguration.current.screenHeightDp.dp
    val panel = Ink.copy(alpha = 0.52f)

    LaunchedEffect(index) {
        if (!state.answered) { stemScroll.scrollTo(0); scroll.scrollTo(0) }
        shownAt = System.currentTimeMillis()
        if (speak && !state.answered) onSay(question.q + ". " + order.joinToString(". ") { question.options[it] }, question.lang)
    }
    // The answer's panel opens at its top: the right answer first, then the explanation.
    // A long stem shows its first lines; a tap opens it, another closes it.
    var open by remember(index) { mutableStateOf(false) }
    val answered = state.answered
    LaunchedEffect(answered) { if (answered) explainScroll.scrollTo(0) }
    val multi = question.multi
    val right = question.correct
    val sayIt = {
        onSay(
            if (!answered) question.q + ". " + order.joinToString(". ") { question.options[it] }
            else order.filter { it in right }.joinToString(". ") { question.options[it] } + ". " + question.explain,
            question.lang,
        )
    }
    // A question with several right answers: tick them all, then check them together.
    val check = {
        val correct = state.chosen == right
        haptics.performHapticFeedback(if (correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        state.checked = true
        open = false
        onAnswer(correct, System.currentTimeMillis() - shownAt)
        if (last) { state.done = true; onDone() }
    }
    var long by remember(index) { mutableStateOf(false) }
    val stemStyle = TextStyle(
        fontFamily = Ui, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
        shadow = TextShadow, textDirection = direction,
    )
    val header: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val meta = listOfNotNull(
                if (questions.size > 1) "${index + 1}/${questions.size}" else null,
                question.source,
            ).joinToString("  ·  ")
            Text(
                meta.uppercase(), color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = Ui, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
                modifier = Modifier.weight(1f),
            )
            if (onAllCircles != null) {
                val label = stringResource(R.string.all_circles)
                Row(
                    Modifier.height(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClickLabel = label, onClick = onAllCircles).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Workspaces, null, tint = Accent, modifier = Modifier.size(14.dp))
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            SmallIcon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), sayIt)
            SmallIcon(Icons.Rounded.Close, stringResource(R.string.close), onClose)
        }
    }
    // The figure the stem refers to, on white as in the book. Tap to see it large.
    val figure: @Composable () -> Unit = {
        question.image?.let { ref ->
            val model = rememberLocalImage(media, ref)
            var large by remember(ref) { mutableStateOf(false) }
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.figure),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable(onClickLabel = stringResource(R.string.figure)) { large = true }
                    .padding(4.dp),
            )
            if (large) {
                Dialog(onDismissRequest = { large = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable { large = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = stringResource(R.string.figure),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                        )
                    }
                }
            }
        }
    }
    val optionRow: @Composable (Int) -> Unit = { option ->
        val isAnswer = option in right
        val isPicked = if (multi) option in state.chosen else option == picked
        val tone = when {
            answered && isAnswer -> Good
            answered && isPicked -> Bad
            multi && isPicked -> Accent
            else -> Color.White
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(if (answered || (multi && isPicked)) tone.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, tone.copy(alpha = if (answered || (multi && isPicked)) 0.7f else 0.30f)), RoundedCornerShape(13.dp))
                .clickable(enabled = !answered) {
                    if (multi) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.chosen = if (option in state.chosen) state.chosen - option else state.chosen + option
                        return@clickable
                    }
                    val correct = option == question.answer
                    haptics.performHapticFeedback(if (correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                    picked = option
                    open = false
                    onAnswer(correct, System.currentTimeMillis() - shownAt)
                    if (last) { state.done = true; onDone() }
                }
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            // A ring to tap; it becomes the tick or the cross once answered.
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                when {
                    answered && isAnswer -> Icon(Icons.Rounded.CheckCircle, null, tint = Good, modifier = Modifier.size(18.dp))
                    answered && isPicked -> Icon(Icons.Rounded.Cancel, null, tint = Bad, modifier = Modifier.size(18.dp))
                    multi && isPicked -> Icon(Icons.Rounded.CheckBox, null, tint = Accent, modifier = Modifier.size(18.dp))
                    multi -> Icon(Icons.Rounded.CheckBoxOutlineBlank, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    else -> Box(Modifier.size(14.dp).border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape))
                }
            }
            Text(
                question.options[option],
                color = Color.White,
                style = TextStyle(fontFamily = Ui, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, textDirection = direction),
                modifier = Modifier.weight(1f),
            )
        }
    }

    // The picture is docked above, whole and uncovered, from the moment the question appears; the
    // question, its options and later the answer live in this fixed panel under it.
    DockPanel {
        Box(Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 4.dp)) { header() }
        val panelScroll = if (answered) explainScroll else scroll
        Box(Modifier.weight(1f)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(panelScroll).padding(horizontal = 12.dp).padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!answered) {
                // A long stem shows three lines, so the options stay in sight; a tap opens it.
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = long || open, onClickLabel = stringResource(R.string.whole_question)) { open = !open }
                        .padding(horizontal = 2.dp),
                ) {
                    Text(
                        question.q, color = Color.White, style = stemStyle.copy(shadow = null),
                        maxLines = if (open) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!open) long = it.hasVisualOverflow },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (long || open) {
                        Text(
                            stringResource(if (open) R.string.show_less else R.string.show_all), color = Accent,
                            style = TextStyle(fontFamily = Ui, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                figure()
                if (multi) {
                    Text(
                        stringResource(R.string.choose_all), color = Accent,
                        style = TextStyle(fontFamily = Ui, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, textDirection = direction),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                order.forEach { optionRow(it) }
                if (multi) {
                    val ready = state.chosen.isNotEmpty()
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(CircleShape)
                            .background(if (ready) Accent else Color.White.copy(alpha = 0.10f))
                            .clickable(enabled = ready) { check() },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.check_answers),
                            color = if (ready) OnAccent else Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                // The question, as one line; a tap shows all of it.
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = stringResource(R.string.whole_question)) { open = !open }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        question.q, color = Color.White.copy(alpha = 0.72f),
                        style = stemStyle.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, shadow = null),
                        maxLines = if (open) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null,
                        tint = Accent, modifier = Modifier.size(18.dp),
                    )
                }
                if (open) figure()
                // The right option(s) first, then the wrong pick(s).
                order.filter { it in right }.forEach { optionRow(it) }
                if (multi) order.filter { it in state.chosen && it !in right }.forEach { optionRow(it) }
                else picked?.takeIf { it != question.answer }?.let { optionRow(it) }
                if (question.explain.isNotBlank()) {
                    Text(
                        question.explain,
                        color = Color.White.copy(alpha = 0.94f),
                        style = TextStyle(fontFamily = Ui, fontSize = 13.sp, lineHeight = 19.sp, textDirection = direction),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    )
                }
            }
        }
        // More lies below (a fifth option, the rest of the explanation): say so, so nothing is missed.
        if (panelScroll.canScrollForward) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(34.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Surface1))),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.more_below), tint = Accent, modifier = Modifier.size(22.dp))
            }
        }
        }
        if (answered) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.09f)))
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                // Watch (or re-watch) the lesson behind this question.
                Row(
                    Modifier.height(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClick = onWatch).padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.watch), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (nextReview != null && state.done) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.Schedule, null, tint = Accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.next_review, nextReview), color = Color.White.copy(alpha = 0.85f), fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, lineHeight = 15.sp)
                    }
                } else Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(40.dp).clip(CircleShape).background(Accent)
                        .clickable { if (last) onNext() else { index++; state.clear() } }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (last) Icon(Icons.Rounded.KeyboardArrowUp, null, tint = OnAccent, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.next), color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
