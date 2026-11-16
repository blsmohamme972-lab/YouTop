package app.youtop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import app.youtop.data.BookInfo
import app.youtop.study.BookStat
import coil3.compose.AsyncImage
import androidx.compose.material.icons.rounded.Psychology
import app.youtop.data.FeedItem
import app.youtop.data.flatten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.study.Dashboard
import app.youtop.study.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * The dashboard: how today is going, what is waiting, the last week, what is coming, and every
 * book's progress. Read-only except for "continue", which goes back to the feed.
 */
@Composable
fun HomeTab(
    vm: MainViewModel,
    onContinue: () -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenTopics: () -> Unit,
    onOpenLesson: (FeedItem) -> Unit = {},
    /** Study one book (its id), or all of them (null), and leave for the feed. */
    onStudyBook: (String?) -> Unit = {},
) {
    val books by vm.books.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val study = settings.study

    // One pass over the library, off the main thread: it holds thousands of lessons.
    val d by produceState(Dashboard(), books, progress, study.excludedBooks, study.book) {
        value = withContext(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            val offset = TimeZone.getDefault().getOffset(now)
            Dashboard.of(books, study.excludedBooks, progress, Scheduler.dayOf(now, offset), now, offset, only = study.book)
        }
    }
    // The book being studied, if one is chosen and still there.
    val current = books.firstOrNull { it.id == study.book }
    // The lessons that keep slipping, hardest first; looked up off the main thread like the rest.
    val slipping by produceState(emptyList<FeedItem>(), d.leeches, books) {
        value = if (d.leeches.isEmpty()) emptyList() else withContext(Dispatchers.Default) {
            val wanted = d.leeches.take(3)
            val found = books.flatten().filter { it.key in wanted }.associateBy { it.key }
            wanted.mapNotNull(found::get)
        }
    }
    val goal = study.dailyGoal.coerceAtLeast(1)
    val streak = progress.liveStreak(progress.day)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Today.
        Card(spacing = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalRing(progress.doneToday, goal, Modifier.size(104.dp))
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(stringResource(R.string.today))
                    Text(
                        if (progress.doneToday >= goal) stringResource(R.string.goal_reached)
                        else pluralStringResource(R.plurals.lessons_to_go, goal - progress.doneToday, goal - progress.doneToday),
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (streak > 0) Accent else Faint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (streak > 0) pluralStringResource(R.plurals.streak_days, streak, streak) else stringResource(R.string.streak_none),
                            color = Faint, fontSize = 13.sp,
                        )
                    }
                }
            }
            PrimaryButton(
                stringResource(if (progress.doneToday == 0) R.string.start_studying else R.string.continue_studying),
                Icons.Rounded.PlayArrow, Modifier.fillMaxWidth(), onClick = onContinue,
            )
            // What that button studies, so it is never a surprise.
            if (books.size > 1 || current != null) {
                Text(
                    current?.let { stringResource(R.string.studying_book, it.title) } ?: stringResource(R.string.studying_all),
                    color = Faint, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // The books, as decks: where each stands, and one button to study it from where it was left.
        if (d.books.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 4.dp).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.my_books), Modifier.weight(1f))
                Text(stringResource(R.string.reps_caption), color = Faint, fontSize = 11.sp)
            }
            d.books.forEach { b ->
                BookCard(
                    vm, b, books.firstOrNull { it.id == b.id },
                    chosen = study.book == b.id || (study.book == null && d.books.size == 1),
                    marked = study.book == b.id && d.books.size > 1,
                    onStudy = { onStudyBook(b.id) }, onOpen = { onOpenBook(b.id) },
                )
            }
            if (d.books.size > 1) {
                val all = study.book == null
                Card(onClick = { onStudyBook(null) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shuffle, null, tint = if (all) Accent else Faint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.study_all), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.study_all_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                        if (all) Icon(Icons.Rounded.CheckCircle, null, tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // The feed is narrowed: say so, because it explains why some lessons never show up.
        if (study.focus.isNotEmpty()) {
            Card(onClick = onOpenTopics) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FilterAlt, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.focus_some, study.focus.size), color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.focus_clear), color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(Accent).clickable { vm.setStudy(study.copy(focus = emptySet())) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }

        // Where the lessons stand.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(d.due, stringResource(R.string.stat_due), Accent, Modifier.weight(1f))
            StatTile(d.learning, stringResource(R.string.stat_learning), Color.White, Modifier.weight(1f))
            StatTile(d.learned, stringResource(R.string.stat_learned), Good, Modifier.weight(1f))
            StatTile(d.fresh, stringResource(R.string.stat_new), Faint, Modifier.weight(1f))
        }

        // How firmly the learned lessons are held, and the ones that will not stick.
        if (d.recall != null || d.leeches.isNotEmpty()) Card {
            d.recall?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionTitle(stringResource(R.string.memory))
                        Text(stringResource(R.string.memory_body), color = Faint, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Text(num((it * 100).roundToInt()) + "%", color = if (it >= 0.8f) Good else Accent, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
                ProgressLine(it, color = if (it >= 0.8f) Good else Accent, height = 5.dp)
            }
            if (d.leeches.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, null, tint = Bad, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(pluralStringResource(R.plurals.slipping, d.leeches.size, d.leeches.size), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text(stringResource(R.string.slipping_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
                slipping.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f))
                            .clickable { onOpenLesson(item) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.lesson.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.PlayArrow, null, tint = Accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // The last seven days.
        if (d.week.any { it > 0 }) Card {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    SectionTitle(stringResource(R.string.last_7_days))
                    Text(pluralStringResource(R.plurals.lessons_done, d.week.sum(), d.week.sum()), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                d.accuracy?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(num((it * 100).roundToInt()) + "%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.answered_right), color = Faint, fontSize = 12.sp)
                    }
                }
            }
            val today = LocalDate.now()
            Bars(
                values = d.week,
                labels = (6 downTo 0).map { dayLetter(today.minusDays(it.toLong()).dayOfWeek) },
                highlight = 6,
                description = stringResource(R.string.last_7_days),
            )
        }

        // What is coming.
        if (d.forecast.any { it > 0 }) {
            Card {
                SectionTitle(stringResource(R.string.coming_up))
                Text(pluralStringResource(R.plurals.reviews_tomorrow, d.forecast[0], d.forecast[0]), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                val today = LocalDate.now()
                Bars(
                    values = d.forecast,
                    labels = (1..7).map { dayLetter(today.plusDays(it.toLong()).dayOfWeek) },
                    highlight = 0,
                    description = stringResource(R.string.coming_up),
                )
            }
        }

    }
}

/**
 * One book as a deck: its cover across the top, how many of its lessons were studied how many
 * times as one coloured bar with the numbers underneath, and the button that studies it.
 */
@Composable
private fun BookCard(vm: MainViewModel, b: BookStat, book: BookInfo?, chosen: Boolean, marked: Boolean, onStudy: () -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, if (marked) Accent else Color.White.copy(alpha = 0.08f), shape)) {
        Box(Modifier.fillMaxWidth().height(128.dp).clickable(onClickLabel = stringResource(R.string.book_lessons), onClick = onOpen)) {
            val model = remember(book?.cover) { vm.media.imageModel(book?.cover) }
            if (model != null) {
                AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(coverBrush(b.id)))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to Surface1.copy(alpha = 0.55f), 1f to Surface1)))
            if (marked) {
                Text(
                    stringResource(R.string.studying_now), color = OnAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(Accent).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            if (b.due > 0) {
                Text(
                    pluralStringResource(R.plurals.due_count, b.due, b.due), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(b.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 25.sp)
                Text(
                    if (b.started > 0) stringResource(R.string.book_progress, b.started, b.total) else pluralStringResource(R.plurals.lessons_count, b.total, b.total),
                    color = Faint, fontSize = 13.sp,
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RepsBar(b.reps)
            RepsLegend(b.reps)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (chosen) PrimaryButton(stringResource(R.string.study_book), Icons.Rounded.PlayArrow, Modifier.weight(1f), onClick = onStudy)
                else SecondaryButton(stringResource(R.string.study_book), Icons.Rounded.PlayArrow, Modifier.weight(1f), onClick = onStudy)
                SecondaryButton(stringResource(R.string.book_lessons), Icons.AutoMirrored.Rounded.List, onClick = onOpen)
            }
        }
    }
}

/** A short name for a weekday. Arabic names all begin with the article, so that is dropped first. */
private fun dayLetter(day: DayOfWeek): String =
    day.getDisplayName(DayStyle.SHORT, Locale.getDefault()).removePrefix("ال").take(3)

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Faint, fontSize = 12.sp)
    }
}

/** Today's lessons against the daily goal. */
@Composable
private fun GoalRing(done: Int, goal: Int, modifier: Modifier = Modifier) {
    val fill = (done.toFloat() / goal).coerceIn(0f, 1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.10f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (fill > 0f) drawArc(Accent, -90f, 360f * fill, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num(done), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
            Text(stringResource(R.string.of_goal, goal), color = Faint, fontSize = 12.sp)
        }
    }
}

/** One headline number. The dot carries the colour; the number stays in plain ink. */
@Composable
private fun StatTile(value: Int, label: String, dot: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(num(value), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(5.dp))
            Text(label, color = Faint, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Seven thin bars on a shared baseline, each with its count above and its day below.
 * One series, so one colour; [highlight] (today, or tomorrow) is the amber one.
 */
@Composable
private fun Bars(values: List<Int>, labels: List<String>, highlight: Int, description: String) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val spoken = description + ": " + values.indices.joinToString { "${labels[it]} ${values[it]}" }
    Row(
        Modifier.fillMaxWidth().height(116.dp).semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(if (v > 0) num(v) else "", color = if (i == highlight) Color.White else Faint, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.width(18.dp).height(if (v > 0) (6 + 50 * v / max).dp else 3.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                        .background(if (v == 0) Color.White.copy(alpha = 0.10f) else if (i == highlight) Accent else Color.White.copy(alpha = 0.38f))
                )
                Spacer(Modifier.height(6.dp))
                Text(labels[i], color = if (i == highlight) Color.White else Faint, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}
