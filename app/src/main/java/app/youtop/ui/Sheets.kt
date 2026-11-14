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
