package app.youtop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** The shelf: every book by its cover, with a search over every lesson. Tapping a book opens its chapters and lessons. */
@Composable
fun BooksTab(
    vm: MainViewModel,
    books: List<BookInfo>,
    saved: Set<String>,
    openId: String?,
    onOpenBook: (String?) -> Unit,
    onOpenLesson: (FeedItem) -> Unit,
) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()
    val now = remember(progress) { System.currentTimeMillis() }
    val open = books.firstOrNull { it.id == openId }
    var query by rememberSaveable { mutableStateOf("") }

    if (open != null) {
        BackHandler { onOpenBook(null) }
        BookDetail(
            vm, open, saved, progress.cards, now,
            inFeed = open.id !in settings.study.excludedBooks,
            onInFeed = { on ->
                val excluded = settings.study.excludedBooks
                vm.setStudy(settings.study.copy(excludedBooks = if (on) excluded - open.id else excluded + open.id))
            },
            onBack = { onOpenBook(null) },
            onOpenLesson = onOpenLesson,
        )
        return
    }

    // Searched off the main thread: the library holds thousands of lessons.
    val wanted = query.trim()
    val found by produceState<List<FeedItem>?>(null, wanted, books, taxonomy) {
        value = if (wanted.length < 2) null else withContext(Dispatchers.Default) { searchLessons(books, wanted, taxonomy.titles) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        SearchField(query, { query = it }, stringResource(R.string.search_hint), Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        val results = found
        when {
            results != null && results.isEmpty() ->
                EmptyState(Icons.Rounded.Search, stringResource(R.string.search_none, wanted))

            results != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "count") {
                    Text(
                        if (results.size >= SEARCH_LIMIT) stringResource(R.string.search_first, SEARCH_LIMIT)
                        else pluralStringResource(R.plurals.lessons_count, results.size, results.size),
                        color = Faint, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
                    )
                }
                items(results, key = { it.key }) { item -> LessonRow(vm, item, item.key in saved, progress.cards[item.key], now, book = item.book.title) { onOpenLesson(item) } }
            }

            books.isEmpty() -> EmptyState(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.no_books_title), stringResource(R.string.empty_feed))

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(books, key = { it.id }) { book ->
                    val lessons = remember(book) { listOf(book).flatten() }
                    val due = remember(book, progress) { lessons.count { isDue(progress.cards[it.key], now) } }
                    val started = remember(book, progress) { lessons.count { (progress.cards[it.key]?.phase ?: Phase.NEW) != Phase.NEW } }
                    val excluded = book.id in settings.study.excludedBooks
                    Column(Modifier.clickable { onOpenBook(book.id) }) {
                        Box {
                            BookCover(vm, book, Modifier.fillMaxWidth().aspectRatio(0.72f))
                            if (due > 0 && !excluded) {
                                Text(
                                    num(due), color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape)
                                        .background(Accent).padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            if (excluded) {
                                Text(
                                    stringResource(R.string.not_in_feed), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 2.dp),
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
                            else pluralStringResource(R.plurals.lessons_count, lessons.size, lessons.size),
                            color = Faint, fontSize = 12.sp,
                        )
                        if (started > 0) ProgressLine(started.toFloat() / lessons.size.coerceAtLeast(1), Modifier.padding(top = 6.dp), height = 3.dp)
                    }
                }
            }
        }
    }
}

const val SEARCH_LIMIT = 200

/**
 * Lessons whose title (either language), number or topic contains every word of [query],
 * in book order, at most [SEARCH_LIMIT].
 */
fun searchLessons(books: List<BookInfo>, query: String, tagTitles: Map<String, String>): List<FeedItem> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val out = ArrayList<FeedItem>()
    for (book in books) for (chapter in book.chapters) for (lesson in chapter.lessons) {
        val hay = buildString {
            append(lesson.title.lowercase()).append(' ')
            lesson.titleAr?.let { append(it).append(' ') }
            append(lesson.id.lowercase()).append(' ')
            for (tag in lesson.tags) append(tagTitles[tag]?.lowercase() ?: tag).append(' ')
        }
        if (words.all { it in hay }) {
            out += FeedItem(book, chapter, lesson)
            if (out.size >= SEARCH_LIMIT) return out
        }
    }
    return out
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(Surface2).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Faint, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = Ui),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, stringResource(R.string.clear_search), tint = Faint,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") },
            )
        }
    }
}

private fun isDue(card: CardState?, now: Long) =
    card != null && app.youtop.study.Scheduler.isDue(card, now, app.youtop.study.Scheduler.dayEnd(now, java.util.TimeZone.getDefault().getOffset(now)))

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
    // With many chapters, show them folded: a list of five thousand lessons is no way to find one.
    val folds = book.chapters.size > 3
    val unfolded = remember(book.id) { mutableStateMapOf<String, Boolean>() }
    val lessons = remember(book) { listOf(book).flatten() }
    val started = remember(book, cards) { lessons.count { (cards[it.key]?.phase ?: Phase.NEW) != Phase.NEW } }
    val learned = remember(book, cards) { lessons.count { cards[it.key]?.phase == Phase.REVIEW } }
    val due = remember(book, cards) { lessons.count { isDue(cards[it.key], now) } }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        item(key = "summary") {
            Card(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.book_progress, started, lessons.size), color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (due > 0) Text(pluralStringResource(R.plurals.due_count, due, due), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                val total = lessons.size.coerceAtLeast(1).toFloat()
                ProgressLine(started / total, second = learned / total, height = 5.dp)
                book.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = Faint, fontSize = 13.sp, lineHeight = 19.sp) }
                Toggle(stringResource(R.string.in_feed), inFeed, stringResource(R.string.in_feed_hint), onInFeed)
            }
        }
        book.chapters.forEach { chapter ->
            val shown = !folds || unfolded[chapter.id] == true
            if (book.chapters.size > 1 || chapter.title.isNotBlank()) {
                item(key = "chapter:${chapter.id}") {
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = folds) { unfolded[chapter.id] = !shown }
                            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionTitle(chapter.title, Modifier.weight(1f))
                        val done = chapter.lessons.count { (cards["${book.id}/${chapter.id}/${it.id}"]?.phase ?: Phase.NEW) != Phase.NEW }
                        Text(ratio(done, chapter.lessons.size), color = Faint, fontSize = 12.sp)
                        if (folds) Icon(if (shown) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Faint, modifier = Modifier.padding(start = 6.dp).size(20.dp))
                    }
                }
            }
            if (shown) {
                items(chapter.lessons, key = { "lesson:${chapter.id}/${it.id}" }) { lesson ->
                    val item = FeedItem(book, chapter, lesson)
                    LessonRow(vm, item, item.key in saved, cards[item.key], now) { onOpenLesson(item) }
                }
            }
        }
    }
}

@Composable
fun LessonRow(vm: MainViewModel, item: FeedItem, saved: Boolean, card: CardState?, now: Long, book: String? = null, onClick: () -> Unit) {
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
            (book ?: item.lesson.titleAr)?.let { Text(it, color = Faint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
