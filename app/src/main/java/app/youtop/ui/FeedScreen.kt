package app.youtop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import app.youtop.FeedEntry
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.FeedItem
import app.youtop.data.SyncState
import app.youtop.data.Track
import app.youtop.player.NarrationMode

private val Speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun FeedScreen(vm: MainViewModel, onOpenLibrary: () -> Unit) {
    val queue = vm.queue
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { queue.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.settledPage, queue.getOrNull(pagerState.settledPage)?.id) {
        vm.activate(pagerState.settledPage)
    }
    LaunchedEffect(Unit) {
        vm.jumps.collect { index ->
            snapshotFlow { queue.size }.first { it > index }
            pagerState.animateScrollToPage(index)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (queue.isEmpty()) {
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
                key = { queue.getOrNull(it)?.id ?: -1L - it },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val entry = queue.getOrNull(page) ?: return@VerticalPager
                LessonPage(
                    entry = entry,
                    active = page == pagerState.settledPage,
                    vm = vm,
                    onNext = { scope.launch { if (page + 1 < queue.size) pagerState.animateScrollToPage(page + 1) } },
                )
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

        val finished by vm.finishedForToday.collectAsStateWithLifecycle()
        if (finished && pagerState.settledPage >= queue.lastIndex && queue.isNotEmpty()) {
            Text(
                stringResource(R.string.done_today),
                color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp)
                    .clip(CircleShape).background(Accent).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        // Today's progress: a ring that fills towards the daily goal, with the streak inside.
        val goal = settings.study.dailyGoal.coerceAtLeast(1)
        val fill by animateFloatAsState((progress.doneToday.toFloat() / goal).coerceIn(0f, 1f), tween(600), label = "goal")
        val streak = progress.liveStreak(progress.day)
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 62.dp, end = 10.dp)
                .size(44.dp).clip(CircleShape).background(Glass)
                .drawBehind {
                    val stroke = 3.dp.toPx()
                    val inset = stroke / 2f + 2.dp.toPx()
                    drawArc(
                        if (fill >= 1f) Accent else Color.White, -90f, 360f * fill, false,
                        topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (streak > 0) {
                Text("$streak", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Rounded.LocalFireDepartment, stringResource(R.string.streak), tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** What the learner did during one appearance of a lesson; becomes its review rating. */
private class ViewLog {
    var since = 0L
    var answered = 0
    var wrong = 0
    var answerMs = 0L
    var finishedNarration = false
}

@Composable
private fun LessonPage(entry: FeedEntry, active: Boolean, vm: MainViewModel, onNext: () -> Unit) {
    val item = entry.item
    val lesson = item.lesson
    val trackIndex = (vm.trackChoice[entry.id] ?: 0).coerceIn(0, (lesson.tracks.size - 1).coerceAtLeast(0))
    val track = lesson.tracks.getOrNull(trackIndex)
    val myKey = "${entry.id}#$trackIndex"

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

    val study = settings.study
    var showQuiz by remember { mutableStateOf(false) }
    var showTranscript by rememberSaveable { mutableStateOf(false) }

    // Quietly note how this viewing went, and hand it to the scheduler when the learner is done with it.
    val log = remember { ViewLog() }
    val report = {
        if (log.since > 0L) {
            vm.finish(
                entry, log.answered, log.wrong,
                if (log.answered > 0) log.answerMs / log.answered else 0L,
                log.finishedNarration, System.currentTimeMillis() - log.since,
            )
        }
    }
    LaunchedEffect(active) {
        if (active) {
            log.since = System.currentTimeMillis()
        } else if (log.since > 0L) {
            report()
            showQuiz = false
        }
    }
    DisposableEffect(Unit) { onDispose { report() } }

    // Story, then explanation, then the question, then the next lesson, without a tap.
    LaunchedEffect(ended) {
        if (!ended) return@LaunchedEffect
        if (trackIndex < lesson.tracks.lastIndex) {
            if (study.autoContinue) vm.selectTrack(entry, trackIndex + 1)
            return@LaunchedEffect
        }
        log.finishedNarration = true
        if (lesson.quiz.isNotEmpty()) {
            if (study.autoContinue) showQuiz = true
        } else if (study.autoAdvance) {
            delay(1200)
            onNext()
        }
    }

    val segment = track?.segments?.getOrNull(segmentIndex)
    val spot = if (mine && !ended && !showQuiz) segment?.spot else null
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
                                .clickable { vm.selectTrack(entry, i) }
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
            visible = active && !playing && track != null && !showQuiz,
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

        AnimatedVisibility(visible = !showQuiz, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize()) {
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
        }

        AnimatedVisibility(
            visible = showQuiz && lesson.quiz.isNotEmpty(),
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 8 },
            exit = fadeOut(tween(200)),
        ) {
            QuizOverlay(
                questions = lesson.quiz,
                shuffle = study.shuffleOptions,
                autoAdvance = study.autoAdvance,
                speak = study.speakQuiz,
                onSay = { text, lang -> vm.narrator.say(text, lang) },
                onAnswer = { correct, tookMs ->
                    log.answered++
                    if (!correct) log.wrong++
                    log.answerMs += tookMs
                },
                onDone = report,
                onNext = { vm.narrator.stopSaying(); onNext() },
                onClose = { vm.narrator.stopSaying(); showQuiz = false },
            )
        }
    }

    if (showTranscript && track != null) {
        TranscriptSheet(
            lesson = lesson,
            trackIndex = trackIndex,
            currentSegment = segmentIndex,
            onSelectTrack = { vm.selectTrack(entry, it) },
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
