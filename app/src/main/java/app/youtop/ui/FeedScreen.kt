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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import app.youtop.data.Lesson
import app.youtop.data.Segment
import app.youtop.data.SyncState
import app.youtop.data.Track
import app.youtop.player.NarrationMode
import app.youtop.study.Tags
import androidx.compose.animation.core.animate
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.Job
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity

private val Speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun FeedScreen(vm: MainViewModel, onOpenLibrary: (LibraryTab?) -> Unit) {
    val queue = vm.queue
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    // A new pager, back at the top, each time the feed starts afresh with another book or other topics.
    // The feed ends, for now, at the first lesson whose question is not answered yet.
    val pagerState = key(vm.generation) { rememberPagerState { vm.reachable() } }
    val scope = rememberCoroutineScope()
    // A swipe away from an unanswered lesson: the page gives a little, then springs back with its question open.
    var pull by remember { mutableFloatStateOf(0f) }
    var springBack by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current
    val hold = remember(pagerState) {
        object : NestedScrollConnection {
            private val limit = with(density) { 140.dp.toPx() }
            private val enough = with(density) { 18.dp.toPx() }
            private fun held() = !pagerState.canScrollForward && vm.holds(pagerState.currentPage)

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Dragging back down first undoes the pull.
                if (pull < 0f && available.y > 0f && source == NestedScrollSource.UserInput) {
                    val back = minOf(available.y, -pull)
                    pull += back
                    return Offset(0f, back)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && held()) {
                    springBack?.cancel()
                    pull = (pull + available.y * 0.5f).coerceAtLeast(-limit)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val flick = available.y < -400f && held() && pagerState.currentPageOffsetFraction == 0f
                if (pull == 0f && !flick) return Velocity.Zero
                if (pull < -enough || flick) queue.getOrNull(pagerState.currentPage)?.let { vm.heldBack.tryEmit(it.id) }
                springBack?.cancel()
                springBack = scope.launch {
                    animate(pull, 0f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f)) { v, _ -> pull = v }
                }
                return available
            }
        }
    }

    LaunchedEffect(pagerState.settledPage, queue.getOrNull(pagerState.settledPage)?.id) {
        vm.activate(pagerState.settledPage)
    }
    LaunchedEffect(pagerState) {
        vm.jumps.collect { index ->
            snapshotFlow { queue.size }.first { it > index }
            pagerState.animateScrollToPage(index)
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        val finished by vm.finishedForToday.collectAsStateWithLifecycle()
        if (queue.isEmpty()) {
            if (loaded) EmptyFeed(vm, finished, sync, Modifier.align(Alignment.Center))
        } else {
            // What shows under the page while it is pulled: why it will not go.
            Text(
                stringResource(R.string.answer_first),
                color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)
                    .graphicsLayer { alpha = (-pull / 120f).coerceIn(0f, 1f) },
            )
            key(pagerState) { VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { queue.getOrNull(it)?.id ?: -1L - it },
                modifier = Modifier.fillMaxSize().nestedScroll(hold).graphicsLayer { translationY = pull },
            ) { page ->
                val entry = queue.getOrNull(page) ?: return@VerticalPager
                LessonPage(
                    entry = entry,
                    active = page == pagerState.settledPage,
                    vm = vm,
                    // Nothing moves on while the library covers the feed.
                    onNext = { if (!vm.covered) scope.launch { if (page + 1 < vm.reachable()) pagerState.animateScrollToPage(page + 1) } },
                )
            } }
        }

        Row(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (sync is SyncState.Running) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            }
            GlassButton(Icons.Rounded.Home, stringResource(R.string.library), { onOpenLibrary(null) }, caption = stringResource(R.string.home))
        }

        if (finished && pagerState.settledPage >= queue.lastIndex && queue.isNotEmpty()) {
            Text(
                stringResource(R.string.done_today),
                color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp)
                    .clip(CircleShape).background(Accent).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        // Today's progress: a ring that fills towards the daily goal, with the streak inside.
        val goal = settings.study.dailyGoal.coerceAtLeast(1)
        val fill by animateFloatAsState((progress.doneToday.toFloat() / goal).coerceIn(0f, 1f), tween(600), label = "goal")
        val streak = progress.liveStreak(progress.day)
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 80.dp, end = 10.dp)
                .size(44.dp).clip(CircleShape).background(Glass)
                .clickable(onClickLabel = stringResource(R.string.today)) { onOpenLibrary(LibraryTab.HOME) }
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

        // Shown until the first swipe, once: how to get to the next lesson.
        val showHint = loaded && !settings.hintSeen && queue.size > 1 && pagerState.settledPage == 0 && !vm.covered && !vm.holds(0)
        LaunchedEffect(pagerState.settledPage) { if (pagerState.settledPage > 0 && !settings.hintSeen) vm.hintSeen() }
        AnimatedVisibility(
            visible = showHint, enter = fadeIn(tween(400)), exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 134.dp),
        ) {
            Row(
                Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.swipe_hint), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** The feed has nothing to show: say why, and offer the one thing that fixes it. */
@Composable
private fun EmptyFeed(vm: MainViewModel, finished: Boolean, sync: SyncState, modifier: Modifier = Modifier) {
    val books by vm.books.collectAsStateWithLifecycle()
    val hasLessons = books.any { b -> b.chapters.any { it.lessons.isNotEmpty() } }
    when {
        finished -> EmptyState(Icons.Rounded.CheckCircle, stringResource(R.string.done_today), stringResource(R.string.done_today_body), modifier) {
            PrimaryButton(stringResource(R.string.keep_going)) { vm.setStudy(vm.settings.value.study.copy(afterGoal = "more")) }
        }

        hasLessons -> EmptyState(Icons.Rounded.FilterAlt, stringResource(R.string.nothing_matches), stringResource(R.string.nothing_matches_body), modifier) {
            PrimaryButton(stringResource(R.string.show_everything)) { vm.clearFilters() }
        }

        else -> EmptyState(
            Icons.Rounded.CloudDownload, stringResource(R.string.no_books_title),
            when (sync) {
                is SyncState.Running -> stringResource(R.string.sync_running)
                is SyncState.Failed -> stringResource(if (sync.offline) R.string.sync_offline_empty else R.string.sync_failed)
                else -> stringResource(R.string.empty_feed)
            },
            modifier,
        ) {
            if (sync is SyncState.Running) CircularProgressIndicator(Modifier.size(24.dp), color = Accent, strokeWidth = 2.5.dp)
            else PrimaryButton(stringResource(R.string.sync_now), Icons.Rounded.Sync) { vm.syncNow() }
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
    val info = item.lesson
    // The title and picture are known at once; narration and questions are read from disk.
    // Keyed on the chapter file too: after a sync replaces it, the page reads the new text.
    // (Read and its result in one value: "read, and it has no question" must never be seen half-way.)
    val loaded by produceState<Pair<Boolean, Lesson?>>(false to null, item.key, item.chapter.source) { value = true to vm.lesson(item) }
    val full = loaded.second
    val read = loaded.first
    val lesson = full ?: Lesson(id = info.id, title = info.title, titleAr = info.titleAr, image = info.image, tags = info.tags)
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
    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()
    val questionFirst = study.questionFirst && info.questions > 0
    var showQuiz by remember { mutableStateOf(false) }
    // Kept here, not inside the overlay, so a question stays answered when it is closed and reopened.
    val quiz = remember { QuizState() }
    // Set once the learner has chosen to watch, so "question first" does not cover the video again.
    var watching by remember { mutableStateOf(false) }
    var showTranscript by rememberSaveable { mutableStateOf(false) }
    // "All circles": every numbered circle of the picture, with their table.
    var showAll by remember { mutableStateOf(false) }
    var pickedMark by remember { mutableStateOf<Int?>(null) }
    val allCircles = { vm.narrator.pause(); pickedMark = null; showAll = true }

    // A lesson whose question cannot be asked after all (its text could not be read) must not hold the feed.
    LaunchedEffect(read, lesson.quiz.size) { if (read && lesson.quiz.isEmpty()) vm.pass(entry.id) }
    // Swiped away unanswered: back here, with the question open, wherever the story had got to.
    LaunchedEffect(entry.id) {
        vm.heldBack.collect { id ->
            if (id == entry.id && info.questions > 0) {
                vm.narrator.pause()
                showAll = false
                showQuiz = true
            }
        }
    }

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
            if (questionFirst && !watching) {
                vm.narrator.pause()
                showQuiz = true
            }
        } else if (log.since > 0L) {
            report()
            showQuiz = false
            showAll = false
            watching = false
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
        if (lesson.quiz.isNotEmpty() && !quiz.done) {
            if (study.autoContinue) showQuiz = true
        } else if (study.autoAdvance) {
            delay(1200)
            onNext()
        }
    }

    val segment = track?.segments?.getOrNull(segmentIndex)
    val spot = if (mine && !ended && !showQuiz && !showAll) segment?.spot else null
    // The circles on the picture: all of them when asked for; under a question only the ones that
    // belong to it, and the correct answer's once it is answered.
    val asked = if (showQuiz) lesson.quiz.getOrNull(quiz.index) else null
    val circles = remember(lesson.marks, showAll, asked, quiz.picked) {
        when {
            showAll -> lesson.marks.numbered(1..lesson.marks.size)
            asked != null -> lesson.marks.numbered(
                asked.marks + (if (quiz.picked != null) asked.answerMarks else emptyList()),
                pulse = if (quiz.picked != null) asked.answerMarks.toSet() else emptySet(),
            )
            else -> emptyList()
        }
    }
    val rtl = track?.let { isRtl(it.lang) } ?: false
    val context = LocalContext.current

    // After the question is answered: when the scheduler will bring this lesson back.
    val progress by vm.progress.collectAsStateWithLifecycle()
    val nextReview = if (!quiz.done) null else progress.cards[item.key]?.takeIf { it.phase != app.youtop.study.Phase.NEW }?.let {
        val now = System.currentTimeMillis()
        android.text.format.DateUtils.getRelativeTimeSpanString(maxOf(it.due, now + 60_000), now, android.text.format.DateUtils.MINUTE_IN_MILLIS).toString()
    }

    // The narration must not start under an open question when the library closes.
    if (active) SideEffect { vm.quizOpen = showQuiz || showAll }

    // Back closes an open question instead of leaving the app.
    BackHandler(enabled = active && showQuiz && !vm.covered) { vm.narrator.stopSaying(); showQuiz = false }
    BackHandler(enabled = active && showAll && !vm.covered) { showAll = false }
    val plain = rememberUpdatedState(!showAll)

    // The picture is downloaded once into the app's own store and shown from there.
    val picture = rememberLocalImage(vm.media, lesson.image)

    Box(Modifier.fillMaxSize()) {
        SpotlightImage(
            model = picture,
            spot = spot,
            seed = item.key.hashCode(),
            marks = circles,
            focus = if (showAll) pickedMark else null,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { if (plain.value) vm.narrator.toggle() } },
        )

        // Soft shading behind the floating text and buttons.
        Box(
            Modifier.fillMaxWidth().height(170.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.62f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(340.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Ink.copy(alpha = 0.62f), 1f to Ink.copy(alpha = 0.86f)))
        )

        // Title and story / explanation switch.
        // Hidden while a question is open: the question has its own buttons up there.
        AnimatedVisibility(
            visible = !showQuiz, enter = fadeIn(tween(200)), exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
        Column(
            Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp, end = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                (if (rtl) lesson.titleAr else null) ?: lesson.title,
                color = Color.White,
                style = TextStyle(fontFamily = Ui, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, shadow = TextShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val topic = Tags.mainTopic(info.tags)?.let { taxonomy.label(it, rtl) }
            if (topic != null) {
                Text(
                    topic.uppercase(),
                    color = Accent,
                    style = TextStyle(fontFamily = Ui, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, shadow = TextShadow),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (lesson.tracks.size > 1) {
                Row(Modifier.clip(CircleShape).background(Glass).padding(3.dp)) {
                    lesson.tracks.forEachIndexed { i, t ->
                        val selected = i == trackIndex
                        Text(
                            trackLabel(t),
                            color = if (selected) OnAccent else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) Accent else Color.Transparent)
                                .clickable { vm.selectTrack(entry, i) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            if (mine && mode == NarrationMode.TEXT) {
                // No voice for this language on the phone: one tap opens the phone's voice settings.
                Text(
                    stringResource(R.string.voice_missing),
                    color = Color.White.copy(alpha = 0.9f),
                    style = TextStyle(fontFamily = Ui, fontSize = 12.sp, lineHeight = 17.sp, shadow = TextShadow),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Glass)
                        .clickable {
                            runCatching {
                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        }

        // Paused indicator.
        AnimatedVisibility(
            visible = active && !playing && track != null && !showQuiz && !showAll,
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

        AnimatedVisibility(visible = !showQuiz && !showAll, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize()) {
                // Action rail.
                Column(
                    Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 10.dp, bottom = 76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (lesson.quiz.isNotEmpty()) {
                        GlassButton(
                            Icons.Rounded.Quiz, stringResource(R.string.quiz),
                            onClick = { vm.narrator.pause(); showQuiz = true },
                            tint = if (ended) Accent else Color.White,
                            caption = stringResource(R.string.quiz),
                        )
                    }
                    if (lesson.marks.isNotEmpty()) {
                        GlassButton(Icons.Rounded.Workspaces, stringResource(R.string.all_circles), onClick = allCircles, caption = stringResource(R.string.circles))
                    }
                    GlassButton(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        stringResource(R.string.save),
                        onClick = { vm.toggleSaved(item.key) },
                        tint = if (saved) Accent else Color.White,
                        caption = stringResource(R.string.save),
                    )
                    if (track != null) {
                        GlassButton(Icons.AutoMirrored.Rounded.Notes, stringResource(R.string.transcript), onClick = { showTranscript = true }, caption = stringResource(R.string.transcript))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(Glass)
                                    .clickable(onClickLabel = stringResource(R.string.speed)) {
                                        vm.setSpeed(Speeds[(Speeds.indexOf(speed) + 1).mod(Speeds.size)])
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(speedLabel(speed), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(stringResource(R.string.speed), color = Color.White, style = TextStyle(fontFamily = Ui, fontSize = 10.sp, fontWeight = FontWeight.Medium, shadow = TextShadow), maxLines = 1)
                        }
                        GlassButton(
                            if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            stringResource(R.string.mute),
                            onClick = { vm.setMuted(!muted) },
                            caption = stringResource(if (muted) R.string.sound_off else R.string.sound_on),
                        )
                    }
                }

                // Caption and transport.
                if (track != null) {
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().navigationBarsPadding()) {
                        AnimatedContent(
                            targetState = segment ?: track.segments.firstOrNull(),
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "caption",
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 70.dp),
                        ) { shown -> Caption(shown, track) }
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
                state = quiz,
                media = vm.media,
                shuffle = study.shuffleOptions,
                speak = study.speakQuiz,
                onSay = { text, lang -> vm.narrator.say(text, lang) },
                onAnswer = { correct, tookMs ->
                    log.answered++
                    if (!correct) log.wrong++
                    log.answerMs += tookMs
                },
                onDone = { vm.pass(entry.id); report() },
                onAllCircles = if (lesson.marks.isNotEmpty()) allCircles else null,
                onNext = { vm.narrator.stopSaying(); onNext() },
                onClose = { vm.narrator.stopSaying(); showQuiz = false },
                nextReview = nextReview,
                onWatch = {
                    vm.narrator.stopSaying()
                    watching = true
                    showQuiz = false
                    vm.selectTrack(entry, 0)
                    vm.narrator.replay()
                },
            )
        }
        AnimatedVisibility(visible = showAll && lesson.marks.isNotEmpty(), enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.94f), exit = fadeOut(tween(150))) {
            MarksTable(
                marks = lesson.marks,
                selected = pickedMark,
                onSelect = { pickedMark = it },
                onClose = { showAll = false },
                // Over a question it starts between the question and the options; otherwise at the bottom.
                startBias = if (showQuiz) 0.42f else 1f,
                modifier = Modifier.navigationBarsPadding(),
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

/**
 * The sentence being spoken. A story is set in the book face; the quotation that closes it stands
 * alone, centred, with where it is from underneath. Direction follows the track's language, not
 * the first letter, so an Arabic sentence that opens with an English term still reads right to left.
 */
@Composable
private fun Caption(segment: Segment?, track: Track) {
    val rtl = isRtl(track.lang)
    val direction = if (rtl) TextDirection.Rtl else TextDirection.Ltr
    val literary = rtl && track.kind == "story"
    val source = segment?.quote
    if (segment != null && source != null) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                segment.text,
                color = Color.White,
                style = TextStyle(
                    fontFamily = if (rtl) Literary else Ui, fontSize = 23.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
                    shadow = TextShadow, textDirection = direction, textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.width(40.dp).height(2.dp).clip(CircleShape).background(Accent))
            Text(
                source,
                color = Accent,
                style = TextStyle(
                    fontFamily = Ui, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium,
                    shadow = TextShadow, textDirection = direction, textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Text(
            segment?.text.orEmpty(),
            color = Color.White,
            style = TextStyle(
                fontFamily = if (literary) Literary else Ui,
                fontSize = if (literary) 21.sp else 17.sp,
                lineHeight = if (literary) 35.sp else 26.sp,
                fontWeight = if (literary) FontWeight.Bold else FontWeight.Medium,
                shadow = TextShadow, textDirection = direction,
            ),
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
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
