package app.youtop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.youtop.R
import app.youtop.data.Lesson
import app.youtop.data.Question

private val SheetColor = Color(0xF214161B)
private val Good = Color(0xFF4ADE80)
private val Bad = Color(0xFFF87171)
private val Faint = Color.White.copy(alpha = 0.6f)
private val ContentDirection = TextStyle(textDirection = TextDirection.Content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSheet(questions: List<Question>, onSay: (String, String) -> Unit, onDismiss: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    var score by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetColor,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.35f),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val q = questions.getOrNull(index)
            if (questions.isEmpty()) {
                Text(stringResource(R.string.no_quiz), color = Faint)
            } else if (q == null) {
                Text(
                    stringResource(R.string.quiz_score, score, questions.size),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SheetButton(stringResource(R.string.retry), filled = false) { index = 0; picked = null; score = 0 }
                    SheetButton(stringResource(R.string.close), filled = true, onClick = onDismiss)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.question_n, index + 1, questions.size), color = Faint, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        val spoken = if (picked == null) {
                            q.q + ". " + q.options.joinToString(". ")
                        } else {
                            q.options[q.answer] + ". " + q.explain
                        }
                        onSay(spoken, q.lang)
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), tint = Color.White)
                    }
                }
                Text(q.q, style = ContentDirection, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                q.options.forEachIndexed { i, option ->
                    val answered = picked != null
                    val color = when {
                        answered && i == q.answer -> Good
                        answered && i == picked -> Bad
                        else -> Color.White.copy(alpha = if (answered) 0.25f else 0.5f)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
                            .background(if (answered && (i == q.answer || i == picked)) color.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable(enabled = !answered) {
                                picked = i
                                if (i == q.answer) score++
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, style = ContentDirection, fontSize = 16.sp, modifier = Modifier.weight(1f),
                            color = if (answered && i != q.answer && i != picked) Faint else Color.White)
                        if (answered && i == q.answer) Icon(Icons.Rounded.CheckCircle, null, tint = Good, modifier = Modifier.size(20.dp))
                        if (answered && i == picked && i != q.answer) Icon(Icons.Rounded.Cancel, null, tint = Bad, modifier = Modifier.size(20.dp))
                    }
                }
                AnimatedVisibility(visible = picked != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val right = picked == q.answer
                        Text(
                            stringResource(if (right) R.string.correct else R.string.wrong),
                            color = if (right) Good else Bad, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        )
                        if (q.explain.isNotBlank()) {
                            Text(q.explain, style = ContentDirection, fontSize = 15.sp, lineHeight = 22.sp,
                                color = Color.White.copy(alpha = 0.88f), modifier = Modifier.fillMaxWidth())
                        }
                        SheetButton(
                            stringResource(if (index == questions.lastIndex) R.string.finish else R.string.next),
                            filled = true,
                        ) { index++; picked = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) Color.White else Color.White.copy(alpha = 0.12f),
            contentColor = if (filled) Color.Black else Color.White,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptSheet(
    lesson: Lesson,
    trackIndex: Int,
    currentSegment: Int,
    onSelectTrack: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val track = lesson.tracks[trackIndex]
    val listState = rememberLazyListState()
    LaunchedEffect(currentSegment, trackIndex) {
        if (currentSegment > 0) listState.animateScrollToItem((currentSegment - 1).coerceAtLeast(0))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetColor,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.35f),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                (if (isRtl(track.lang)) lesson.titleAr else null) ?: lesson.title,
                style = ContentDirection, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(),
            )
            if (lesson.tracks.size > 1) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lesson.tracks.forEachIndexed { i, t ->
                        val selected = i == trackIndex
                        Text(
                            trackLabel(t),
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(CircleShape)
                                .background(if (selected) Color.White else Color.White.copy(alpha = 0.12f))
                                .clickable { onSelectTrack(i) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                itemsIndexed(track.segments) { i, segment ->
                    val current = i == currentSegment
                    Text(
                        segment.text,
                        style = ContentDirection,
                        fontSize = 17.sp, lineHeight = 28.sp,
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (current) Accent else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { onSeek(i) }.padding(vertical = 7.dp, horizontal = 4.dp),
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
