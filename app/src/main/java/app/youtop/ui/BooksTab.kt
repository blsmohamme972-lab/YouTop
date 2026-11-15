package app.youtop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.BookInfo
import app.youtop.data.FeedItem
import app.youtop.data.flatten
import app.youtop.study.CardState
import app.youtop.study.Phase
import coil3.compose.AsyncImage
import kotlin.math.abs

private val CoverTints = listOf(
    listOf(Color(0xFF5B3DF5), Color(0xFF0FB5BA)),
    listOf(Color(0xFFB5179E), Color(0xFFF77F00)),
    listOf(Color(0xFF0077B6), Color(0xFF90E0EF)),
    listOf(Color(0xFF2D6A4F), Color(0xFFB7E4C7)),
)

/** The shelf: every book by its cover. Tapping one opens its chapters and lessons. */
@Composable
fun BooksTab(vm: MainViewModel, books: List<BookInfo>, saved: Set<String>, onOpenLesson: (FeedItem) -> Unit) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val now = remember(progress) { System.currentTimeMillis() }
    val open = books.firstOrNull { it.id == openId }

    if (open != null) {
        BackHandler { openId = null }
        BookDetail(
            vm, open, saved, progress.cards, now,
            inFeed = open.id !in settings.study.excludedBooks,
            onInFeed = { on ->
                val excluded = settings.study.excludedBooks
                vm.setStudy(settings.study.copy(excludedBooks = if (on) excluded - open.id else excluded + open.id))
            },
            onBack = { openId = null },
            onOpenLesson = onOpenLesson,
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.navigationBarsPadding(),
    ) {
        items(books, key = { it.id }) { book ->
            val lessons = remember(book) { listOf(book).flatten() }
            val due = lessons.count { isDue(progress.cards[it.key], now) }
            val started = lessons.count { (progress.cards[it.key]?.phase ?: Phase.NEW) != Phase.NEW }
            val excluded = book.id in settings.study.excludedBooks
            Column(Modifier.clickable { openId = book.id }) {
                Box {
                    BookCover(vm, book, Modifier.fillMaxWidth().aspectRatio(0.72f))
                    if (due > 0 && !excluded) {
                        Text(
                            "$due", color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape)
                                .background(Accent).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    book.title, color = if (excluded) Faint else Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (started > 0) stringResource(R.string.book_progress, started, lessons.size)
                    else stringResource(R.string.lessons_count, lessons.size),
                    color = Faint, fontSize = 12.sp,
                )
                if (started > 0) {
                    // How far into the book the learner is.
                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(started.toFloat() / lessons.size.coerceAtLeast(1)).height(3.dp).clip(CircleShape).background(Accent))
                    }
                }
            }
        }
    }
}

private fun isDue(card: CardState?, now: Long) = card != null && card.phase != Phase.NEW && card.due <= now

@Composable
private fun BookCover(vm: MainViewModel, book: BookInfo, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val tint = CoverTints[abs(book.id.hashCode()) % CoverTints.size]
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(tint))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
    ) {
        val model = remember(book.cover) { vm.media.imageModel(book.cover) }
        if (model != null) {
            AsyncImage(model = model, contentDescription = book.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else if (showTitle) {
            Text(
                book.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
    }
}

@Composable
private fun BookDetail(
    vm: MainViewModel,
    book: BookInfo,
    saved: Set<String>,
    cards: Map<String, CardState>,
    now: Long,
    inFeed: Boolean,
    onInFeed: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenLesson: (FeedItem) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.navigationBarsPadding()) {
        item(key = "header") {
            Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
                Spacer(Modifier.width(14.dp))
                BookCover(vm, book, Modifier.width(64.dp).aspectRatio(0.72f), showTitle = false)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    book.titleAr?.let { Text(it, color = Faint, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        item(key = "feed") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.in_feed), color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(checked = inFeed, onCheckedChange = onInFeed)
            }
        }
        book.chapters.forEach { chapter ->
            if (book.chapters.size > 1 || chapter.title.isNotBlank()) {
                item(key = "chapter:${chapter.id}") {
                    Text(
                        chapter.title.uppercase(),
                        color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 4.dp),
                    )
                }
            }
            items(chapter.lessons, key = { "lesson:${chapter.id}/${it.id}" }) { lesson ->
                val item = FeedItem(book, chapter, lesson)
                LessonRow(vm, item, item.key in saved, cards[item.key], now) { onOpenLesson(item) }
            }
        }
    }
}

@Composable
fun LessonRow(vm: MainViewModel, item: FeedItem, saved: Boolean, card: CardState?, now: Long, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 42.dp, height = 64.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f))) {
            AsyncImage(
                // The stored picture when there is one (see MediaCache.thumbModel).
                model = remember(item.lesson.image) { vm.media.thumbModel(item.lesson.image) },
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.lesson.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.lesson.titleAr?.let { Text(it, color = Faint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (saved) Icon(Icons.Rounded.Bookmark, null, tint = Accent, modifier = Modifier.size(18.dp))
        // A dot for where the lesson stands: due now, learned, or not started (no dot).
        val dot = when {
            isDue(card, now) -> Accent
            card != null && card.phase == Phase.REVIEW -> Color(0xFF4ADE80)
            card != null && card.phase != Phase.NEW -> Color.White.copy(alpha = 0.6f)
            else -> null
        }
        if (dot != null) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        }
    }
}
