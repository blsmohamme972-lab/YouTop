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
    /** The learner asked for time (tapped, scrolled, opened the figure): no automatic move to the next lesson. */
    var hold by mutableStateOf(false)
    /** True once the last question has been answered. */
    var done by mutableStateOf(false)
    /** The shuffled order each question was first shown in, so it does not change when reopened. */
    val orders = HashMap<Int, List<Int>>()
}

/**
 * The lesson's questions, laid over the picture: the image stays visible through a soft shade
 * and translucent answer chips. Reports each answer, then offers (or automatically moves to)
 * the next lesson.
 */
@Composable
fun QuizOverlay(
    questions: List<Question>,
    state: QuizState,
    media: MediaCache,
    shuffle: Boolean,
    autoAdvance: Boolean,
    speak: Boolean,
    onSay: (String, String) -> Unit,
    onAnswer: (correct: Boolean, tookMs: Long) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onWatch: () -> Unit,
    /** Once the lesson is rated: when it comes back ("in 4 days"), so the scheduling is not a mystery. */
    nextReview: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    var index by state::index
    var picked by state::picked
    var shownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hold by state::hold
    // A sync can replace the lesson while its question is open: never index past what is there now.
    val question = questions.getOrNull(index.coerceIn(0, (questions.size - 1).coerceAtLeast(0))) ?: return
    val order = remember(index, question) {
        state.orders[index]?.takeIf { it.size == question.options.size } ?: optionOrder(question, shuffle).also { state.orders[index] = it }
    }
    val last = index >= questions.lastIndex
    val countdown = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    val direction = if (isRtl(question.lang)) TextDirection.Rtl else TextDirection.Ltr

    LaunchedEffect(index) {
        if (picked == null) scroll.scrollTo(0) // a new question starts at its stem, not where the last one was left
        shownAt = System.currentTimeMillis()
        if (speak && picked == null) onSay(question.q + ". " + order.joinToString(". ") { question.options[it] }, question.lang)
    }
    LaunchedEffect(picked, hold) {
        countdown.snapTo(0f)
        val answer = picked ?: return@LaunchedEffect
        if (!last || !autoAdvance || hold) return@LaunchedEffect
        val reading = (3000 + 55 * question.explain.length).coerceAtMost(25_000)
        countdown.animateTo(1f, tween(if (answer == question.answer) reading else reading * 3 / 2, easing = LinearEasing))
        onNext()
    }
    // After an answer, bring the explanation and the buttons into view: with a long question
    // they open below the bottom of the screen.
    LaunchedEffect(picked) {
        if (picked != null) {
            delay(350)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }
    // Reading on by scrolling means "do not move on yet".
    LaunchedEffect(scroll) {
        scroll.interactionSource.interactions.collect { if (it is DragInteraction.Start) hold = true }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Ink.copy(alpha = 0.25f), 0.3f to Ink.copy(alpha = 0.55f), 1f to Ink.copy(alpha = 0.90f)))
            // Taps here must not reach the picture underneath; a tap also stops the auto-advance.
            .pointerInput(Unit) { detectTapGestures { hold = true } }
    ) {
        Row(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassButton(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), onClick = {
                hold = true
                val answer = picked
                onSay(
                    if (answer == null) question.q + ". " + order.joinToString(". ") { question.options[it] }
                    else question.options[question.answer] + ". " + question.explain,
                    question.lang,
                )
            })
            GlassButton(Icons.Rounded.Close, stringResource(R.string.close), onClose)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        ) {
            val meta = listOfNotNull(
                if (questions.size > 1) "${index + 1}/${questions.size}" else null,
                question.source,
            ).joinToString("  ·  ")
            if (meta.isNotEmpty()) {
                Text(
                    meta.uppercase(), color = Accent,
                    style = TextStyle(fontFamily = Ui, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, shadow = TextShadow),
                )
            }
            Text(
                question.q,
                color = Color.White,
                style = TextStyle(
                    fontFamily = Ui, fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold,
                    shadow = TextShadow, textDirection = direction,
                ),
                modifier = Modifier.fillMaxWidth(),
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
                        .heightIn(min = 90.dp, max = 240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable(onClickLabel = stringResource(R.string.figure)) { hold = true; large = true }
                        .padding(6.dp),
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
                    if (answered && isAnswer) 1.03f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pop",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                answered && (isAnswer || isPicked) -> tone.copy(alpha = 0.26f)
                                answered -> Color.White.copy(alpha = 0.05f)
                                else -> Color.White.copy(alpha = 0.13f)
                            }
                        )
                        .border(
                            BorderStroke(1.dp, tone.copy(alpha = if (answered && !isAnswer && !isPicked) 0.12f else 0.45f)),
                            RoundedCornerShape(16.dp),
                        )
                        .clickable(enabled = !answered) {
                            val correct = option == question.answer
                            haptics.performHapticFeedback(if (correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                            picked = option
                            onAnswer(correct, System.currentTimeMillis() - shownAt)
                            if (last) { state.done = true; onDone() }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // A ring to tap; it becomes the tick or the cross once answered.
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        when {
                            answered && isAnswer -> Icon(Icons.Rounded.CheckCircle, null, tint = Good, modifier = Modifier.size(22.dp))
                            answered && isPicked -> Icon(Icons.Rounded.Cancel, null, tint = Bad, modifier = Modifier.size(22.dp))
                            else -> Box(
                                Modifier.size(18.dp).border(1.5.dp, Color.White.copy(alpha = if (answered) 0.25f else 0.7f), CircleShape)
                            )
                        }
                    }
                    Text(
                        question.options[option],
                        color = Color.White.copy(alpha = if (answered && !isAnswer && !isPicked) 0.55f else 1f),
                        style = TextStyle(fontFamily = Ui, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, shadow = TextShadow, textDirection = direction),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedVisibility(visible = picked != null, enter = fadeIn() + expandVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (question.explain.isNotBlank()) {
                        Text(
                            question.explain,
                            color = Color.White.copy(alpha = 0.92f),
                            style = TextStyle(fontFamily = Ui, fontSize = 15.sp, lineHeight = 23.sp, shadow = TextShadow, textDirection = direction),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (nextReview != null && state.done) {
                        Row(
                            Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Schedule, null, tint = Accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.next_review, nextReview), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Watch (or re-watch) the lesson behind this question.
                        Row(
                            Modifier.height(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                                .clickable(onClick = onWatch).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.watch), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(Accent)
                                .drawBehind { drawRect(Color.White.copy(alpha = 0.38f), size = Size(size.width * countdown.value, size.height)) }
                                .clickable {
                                    if (last) onNext() else { index++; picked = null; hold = false }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (last) Icon(Icons.Rounded.KeyboardArrowUp, null, tint = OnAccent, modifier = Modifier.size(22.dp))
                                Text(stringResource(R.string.next), color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
