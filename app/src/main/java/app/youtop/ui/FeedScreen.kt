package app.youtop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.FeedItem
import app.youtop.data.SyncState
import app.youtop.data.Track
import app.youtop.player.NarrationMode

private val Speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun FeedScreen(vm: MainViewModel, onOpenLibrary: () -> Unit) {
    val items by vm.feed.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { items.size }
    val latestItems by rememberUpdatedState(items)

    LaunchedEffect(pagerState.settledPage, items) {
        vm.activate(items.getOrNull(pagerState.settledPage))
    }
    LaunchedEffect(Unit) {
        vm.jumps.collect { key ->
            val index = latestItems.indexOfFirst { it.key == key }
            if (index >= 0) pagerState.scrollToPage(index)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isEmpty()) {
            if (loaded) {
                Text(
                    stringResource(R.string.empty_feed),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { items[it].key },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                LessonPage(items[page], active = page == pagerState.settledPage, vm = vm)
            }
        }

        Row(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (sync is SyncState.Running) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            }
            GlassButton(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.library), onOpenLibrary)
        }
    }
}

@Composable
private fun LessonPage(item: FeedItem, active: Boolean, vm: MainViewModel) {
    val lesson = item.lesson
    val trackIndex = (vm.trackChoice[item.key] ?: 0).coerceIn(0, (lesson.tracks.size - 1).coerceAtLeast(0))
    val track = lesson.tracks.getOrNull(trackIndex)
    val myKey = "${item.key}#$trackIndex"

    // Read the narration through derived states so the page only recomposes when
    // something it shows changes, not on every progress tick.
    val narration = vm.narrator.state.collectAsStateWithLifecycle()
    val mine by remember(myKey, active) { derivedStateOf { active && narration.value.key == myKey } }
    val segmentIndex by remember(myKey, active) { derivedStateOf { if (active && narration.value.key == myKey) narration.value.segment else -1 } }
    val playing by remember(myKey, active) { derivedStateOf { active && narration.value.key == myKey && narration.value.playing } }
    val ended by remember(myKey, active) { derivedStateOf { active && narration.value.key == myKey && narration.value.ended } }
    val mode by remember { derivedStateOf { narration.value.mode } }
    val speed by remember { derivedStateOf { narration.value.speed } }
    val muted by remember { derivedStateOf { narration.value.muted } }

    val settings by vm.settings.collectAsStateWithLifecycle()
    val saved = item.key in settings.saved

    var showQuiz by rememberSaveable { mutableStateOf(false) }
    var showTranscript by rememberSaveable { mutableStateOf(false) }

    val segment = track?.segments?.getOrNull(segmentIndex)
    val spot = if (mine && !ended) segment?.spot else null
    val rtl = track?.let { isRtl(it.lang) } ?: false

    Box(Modifier.fillMaxSize()) {
        SpotlightImage(
            model = remember(lesson.image) { vm.media.imageModel(lesson.image) },
            spot = spot,
            seed = item.key.hashCode(),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { vm.narrator.toggle() } },
        )

        // Soft shading behind the floating text and buttons.
        Box(
            Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))))
        )

        // Title and story / explanation switch.
        Column(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 10.dp, end = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                (if (rtl) lesson.titleAr else null) ?: lesson.title,
                color = Color.White,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, shadow = TextShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (lesson.tracks.size > 1) {
                Row(Modifier.clip(CircleShape).background(Glass).padding(3.dp)) {
                    lesson.tracks.forEachIndexed { i, t ->
                        val selected = i == trackIndex
                        Text(
                            trackLabel(t),
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { vm.selectTrack(item, i) }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            if (mine && mode == NarrationMode.TEXT) {
                Text(
                    stringResource(R.string.voice_missing),
                    color = Color.White.copy(alpha = 0.85f),
                    style = TextStyle(fontSize = 11.sp, shadow = TextShadow),
                )
            }
        }

        // Paused indicator.
        AnimatedVisibility(
            visible = active && !playing && track != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.2f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Glass), contentAlignment = Alignment.Center) {
                Icon(
                    if (ended) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp),
                )
            }
        }

        // Action rail.
        Column(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 10.dp, bottom = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (lesson.quiz.isNotEmpty()) {
                GlassButton(
                    Icons.Rounded.Quiz, stringResource(R.string.quiz),
                    onClick = { vm.narrator.pause(); showQuiz = true },
                    tint = if (ended) Accent else Color.White,
                    caption = stringResource(R.string.quiz),
                )
            }
            GlassButton(
                if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                stringResource(R.string.save),
                onClick = { vm.toggleSaved(item.key) },
                tint = if (saved) Accent else Color.White,
            )
            if (track != null) {
                GlassButton(Icons.AutoMirrored.Rounded.Notes, stringResource(R.string.transcript), onClick = { showTranscript = true })
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Glass)
                        .clickable(onClickLabel = stringResource(R.string.speed)) {
                            vm.setSpeed(Speeds[(Speeds.indexOf(speed) + 1).mod(Speeds.size)])
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(speedLabel(speed), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                GlassButton(
                    if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    stringResource(R.string.mute),
                    onClick = { vm.setMuted(!muted) },
                )
            }
        }

        // Caption and transport.
        if (track != null) {
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().navigationBarsPadding()) {
                AnimatedContent(
                    targetState = segment?.text ?: track.segments.firstOrNull()?.text.orEmpty(),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "caption",
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 70.dp),
                ) { text ->
                    Text(
                        text,
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium,
                            shadow = TextShadow, textDirection = TextDirection.Content,
                        ),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransportIcon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (playing) R.string.pause else R.string.play),
                    ) { vm.narrator.toggle() }
                    SeekBar(
                        progress = { if (mine) narration.value.progress else 0f },
                        onSeek = { vm.narrator.seekTo(it) },
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    )
                    TransportIcon(Icons.Rounded.Replay, stringResource(R.string.replay)) { vm.narrator.replay() }
                }
            }
        }
    }

    if (showQuiz) {
        QuizSheet(
            questions = lesson.quiz,
            onSay = { text, lang -> vm.narrator.say(text, lang) },
            onDismiss = { vm.narrator.stopSaying(); showQuiz = false },
        )
    }
    if (showTranscript && track != null) {
        TranscriptSheet(
            lesson = lesson,
            trackIndex = trackIndex,
            currentSegment = segmentIndex,
            onSelectTrack = { vm.selectTrack(item, it) },
            onSeek = { vm.narrator.seekToSegment(it) },
            onDismiss = { showTranscript = false },
        )
    }
}

@Composable
private fun TransportIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

fun trackLabel(track: Track): String {
    val arabic = track.lang.startsWith("ar")
    return when (track.kind) {
        "story" -> if (arabic) "القصة" else "Story"
        else -> if (arabic) "الشرح" else "Explain"
    }
}

private fun speedLabel(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()) + "×"
