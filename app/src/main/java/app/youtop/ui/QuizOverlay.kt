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
import app.youtop.data.Question

private val Good = Color(0xFF4ADE80)
private val Bad = Color(0xFFF87171)

/** Options such as "all of the above" only make sense in their printed order. */
private val Positional = Regex(
    """\b(above|both|all of|none of|neither|[a-e] (and|&|or) [a-e])\b|ما سبق|مما سبق|كلاهما|كل ما ذكر|جميع ما ذكر""",
    RegexOption.IGNORE_CASE,
)

fun optionOrder(question: Question, shuffle: Boolean): List<Int> =
    if (shuffle && question.options.none { Positional.containsMatchIn(it) }) question.options.indices.shuffled()
    else question.options.indices.toList()

/**
 * The lesson's questions, laid over the picture: the image stays visible through a soft shade
 * and translucent answer chips. Reports each answer, then offers (or automatically moves to)
 * the next lesson.
 */
@Composable
fun QuizOverlay(
    questions: List<Question>,
    shuffle: Boolean,
    autoAdvance: Boolean,
    speak: Boolean,
    onSay: (String, String) -> Unit,
    onAnswer: (correct: Boolean, tookMs: Long) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var shownAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hold by remember { mutableStateOf(false) }
    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val order = remember(index) { optionOrder(question, shuffle) }
    val last = index == questions.lastIndex
    val countdown = remember { Animatable(0f) }

    LaunchedEffect(index) {
        shownAt = System.currentTimeMillis()
        if (speak) onSay(question.q + ". " + order.joinToString(". ") { question.options[it] }, question.lang)
    }
    LaunchedEffect(picked, hold) {
        countdown.snapTo(0f)
        val answer = picked ?: return@LaunchedEffect
        if (!last || !autoAdvance || hold) return@LaunchedEffect
        val reading = (2500 + 45 * question.explain.length).coerceAtMost(12_000)
        countdown.animateTo(1f, tween(if (answer == question.answer) reading else reading * 3 / 2, easing = LinearEasing))
        onNext()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.10f), 0.35f to Color.Black.copy(alpha = 0.30f), 1f to Color.Black.copy(alpha = 0.82f)))
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
                .verticalScroll(rememberScrollState(), reverseScrolling = false)
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        ) {
            val meta = listOfNotNull(
                if (questions.size > 1) "${index + 1}/${questions.size}" else null,
                question.source,
            ).joinToString("  ·  ")
            if (meta.isNotEmpty()) {
                Text(meta, color = Color.White.copy(alpha = 0.7f), style = TextStyle(fontSize = 12.sp, shadow = TextShadow))
            }
            Text(
                question.q,
                color = Color.White,
                style = TextStyle(
                    fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold,
                    shadow = TextShadow, textDirection = TextDirection.Content,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
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
                            if (last) onDone()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        question.options[option],
                        color = Color.White.copy(alpha = if (answered && !isAnswer && !isPicked) 0.55f else 1f),
                        style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, shadow = TextShadow, textDirection = TextDirection.Content),
                        modifier = Modifier.weight(1f),
                    )
                    if (answered && isAnswer) Icon(Icons.Rounded.CheckCircle, null, tint = Good, modifier = Modifier.size(20.dp))
                    if (answered && isPicked && !isAnswer) Icon(Icons.Rounded.Cancel, null, tint = Bad, modifier = Modifier.size(20.dp))
                }
            }
            AnimatedVisibility(visible = picked != null, enter = fadeIn() + expandVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (question.explain.isNotBlank()) {
                        Text(
                            question.explain,
                            color = Color.White.copy(alpha = 0.92f),
                            style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, shadow = TextShadow, textDirection = TextDirection.Content),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .drawBehind { drawRect(Color.White.copy(alpha = 0.22f), size = Size(size.width * countdown.value, size.height)) }
                            .clickable {
                                if (last) onNext() else { index++; picked = null; hold = false }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (last) Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text(stringResource(R.string.next), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
