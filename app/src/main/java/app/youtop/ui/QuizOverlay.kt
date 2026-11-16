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
    /** True once the last question has been answered. */
    var done by mutableStateOf(false)
    /** The shuffled order each question was first shown in, so it does not change when reopened. */
    val orders = HashMap<Int, List<Int>>()
}

/**
 * The lesson's questions, laid over the picture without hiding it: the question sits at the top
 * and the options at the bottom, both small and translucent, and the middle of the picture stays
 * free for the circles that belong to the question. Reports each answer; after the last one the
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
        if (picked == null) { stemScroll.scrollTo(0); scroll.scrollTo(0) }
        shownAt = System.currentTimeMillis()
        if (speak && picked == null) onSay(question.q + ". " + order.joinToString(". ") { question.options[it] }, question.lang)
    }
    // After an answer, bring the explanation and the buttons into view.
    LaunchedEffect(picked) {
        if (picked != null) {
            explainScroll.scrollTo(0)
            delay(350)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Taps here must not reach the picture underneath (they would start the narration).
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // ---- top: the question ----
        Column(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 8.dp, end = 64.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(panel)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val meta = listOfNotNull(
                    if (questions.size > 1) "${index + 1}/${questions.size}" else null,
                    question.source,
                ).joinToString("  ·  ")
                Text(
                    meta.uppercase(), color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = Ui, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, shadow = TextShadow),
                    modifier = Modifier.weight(1f),
                )
                if (onAllCircles != null) {
                    val label = stringResource(R.string.all_circles)
                    Row(
                        Modifier.height(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                            .clickable(onClickLabel = label, onClick = onAllCircles).padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Workspaces, null, tint = Accent, modifier = Modifier.size(14.dp))
                        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                SmallIcon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen)) {
                    val answer = picked
                    onSay(
                        if (answer == null) question.q + ". " + order.joinToString(". ") { question.options[it] }
                        else question.options[question.answer] + ". " + question.explain,
                        question.lang,
                    )
                }
                SmallIcon(Icons.Rounded.Close, stringResource(R.string.close), onClose)
            }
            Text(
                question.q,
                color = Color.White,
                style = TextStyle(
                    fontFamily = Ui, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                    shadow = TextShadow, textDirection = direction,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(max = screen * 0.24f).verticalScroll(stemScroll).padding(end = 4.dp),
            )
            // The figure the stem refers to, on white as in the book. Tap to see it large.
            question.image?.let { ref ->
                val figure = rememberLocalImage(media, ref)
                var large by remember(ref) { mutableStateOf(false) }
                AsyncImage(
                    model = figure,
                    contentDescription = stringResource(R.string.figure),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp)
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
                                model = figure,
                                contentDescription = stringResource(R.string.figure),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                            )
                        }
                    }
                }
            }
        }

        // ---- bottom: the options, then what comes after the answer ----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = screen * 0.58f)
                .verticalScroll(scroll)
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
        ) {
            order.forEach { option ->
                val answered = picked != null
                val isAnswer = option == question.answer
                val isPicked = option == picked
                val tone = when {
                    answered && isAnswer -> Good
                    answered && isPicked -> Bad
                    else -> Color.White
                }
                val pop by animateFloatAsState(
                    if (answered && isAnswer) 1.02f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pop",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(RoundedCornerShape(13.dp))
                        .background(panel)
                        .background(if (answered && (isAnswer || isPicked)) tone.copy(alpha = 0.24f) else Color.Transparent)
                        .border(
                            BorderStroke(1.dp, tone.copy(alpha = if (answered && !isAnswer && !isPicked) 0.10f else if (answered) 0.7f else 0.30f)),
                            RoundedCornerShape(13.dp),
                        )
                        .clickable(enabled = !answered) {
                            val correct = option == question.answer
                            haptics.performHapticFeedback(if (correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                            picked = option
                            onAnswer(correct, System.currentTimeMillis() - shownAt)
                            if (last) { state.done = true; onDone() }
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    // A ring to tap; it becomes the tick or the cross once answered.
                    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        when {
                            answered && isAnswer -> Icon(Icons.Rounded.CheckCircle, null, tint = Good, modifier = Modifier.size(18.dp))
                            answered && isPicked -> Icon(Icons.Rounded.Cancel, null, tint = Bad, modifier = Modifier.size(18.dp))
                            else -> Box(
                                Modifier.size(14.dp).border(1.5.dp, Color.White.copy(alpha = if (answered) 0.25f else 0.7f), CircleShape)
                            )
                        }
                    }
                    Text(
                        question.options[option],
                        color = Color.White.copy(alpha = if (answered && !isAnswer && !isPicked) 0.6f else 1f),
                        style = TextStyle(fontFamily = Ui, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, shadow = TextShadow, textDirection = direction),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedVisibility(visible = picked != null, enter = fadeIn() + expandVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (question.explain.isNotBlank()) {
                        // The long explanation, small, in its own window: it scrolls instead of covering the picture.
                        Text(
                            question.explain,
                            color = Color.White.copy(alpha = 0.94f),
                            style = TextStyle(fontFamily = Ui, fontSize = 12.5.sp, lineHeight = 18.sp, shadow = TextShadow, textDirection = direction),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(panel)
                                .heightIn(max = 118.dp)
                                .verticalScroll(explainScroll)
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Watch (or re-watch) the lesson behind this question.
                        Row(
                            Modifier.height(38.dp).clip(CircleShape).background(panel).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable(onClick = onWatch).padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.watch), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (nextReview != null && state.done) {
                            Row(
                                Modifier.weight(1f).height(38.dp).clip(CircleShape).background(panel).padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Schedule, null, tint = Accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(stringResource(R.string.next_review, nextReview), color = Color.White.copy(alpha = 0.92f), fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.height(38.dp).clip(CircleShape).background(Accent)
                                .clickable { if (last) onNext() else { index++; picked = null } }
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
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
