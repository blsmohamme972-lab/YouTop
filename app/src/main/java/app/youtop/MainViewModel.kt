package app.youtop

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.youtop.data.Backup
import app.youtop.data.ContentRepository
import app.youtop.data.DriveClient
import app.youtop.data.FeedItem
import app.youtop.data.Feedback
import app.youtop.data.Note
import app.youtop.data.Lesson
import app.youtop.data.MediaCache
import app.youtop.data.Prefs
import app.youtop.data.Settings
import app.youtop.data.flatten
import app.youtop.player.Narrator
import app.youtop.study.Phase
import app.youtop.study.ProgressStore
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import app.youtop.study.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** One appearance of a lesson in the feed. The same lesson comes back under a new [id] when it is due again. */
data class FeedEntry(val id: Long, val item: FeedItem)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val drive = DriveClient(http, BuildConfig.DRIVE_API_KEY, BuildConfig.CONTENT_URL)
    private val prefs = Prefs(app)
    private val store = ProgressStore(app, viewModelScope)

    val media = MediaCache(app, drive)
    private val repo = ContentRepository(app, drive, media)
    val narrator = Narrator(app, viewModelScope)

    val books = repo.books
    val taxonomy = repo.taxonomy
    val sync = repo.sync
    val progress = store.progress
    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** False until the bundled and cached lessons have been read from disk. */
    val loaded = MutableStateFlow(false)

    /**
     * The feed. It is built a step ahead of the learner by the scheduler, so what comes next always
     * reflects how the lessons before it went.
     */
    val queue = mutableStateListOf<FeedEntry>()

    /** True when the scheduler has nothing more to show today (only with "stop when done"). */
    val finishedForToday = MutableStateFlow(false)

    /** Which narration (story / explanation) is playing for each feed entry. */
    val trackChoice = mutableStateMapOf<Long, Int>()

    /** True while the library covers the feed: nothing should start playing or move on underneath it. */
    var covered by mutableStateOf(true)

    /** Goes up each time the feed starts afresh (another book or other topics were chosen): the pager begins again at the top. */
    var generation by mutableStateOf(0)
        private set

    /** True while the lesson on screen shows its question: the narration must not start underneath it. */
    var quizOpen = false

    /** A new choice of book is on its way to the feed; nothing of the old feed should start playing meanwhile. */
    private var choosing = false

    /**
     * Feed entries the learner may swipe past: their question was answered (or they have none that
     * can be asked). A lesson with a question holds the feed until then.
     */
    private val passed = mutableStateMapOf<Long, Boolean>()

    /** A lesson (feed entry id) the learner tried to swipe away unanswered: it opens its question. */
    val heldBack = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    fun pass(entryId: Long) { passed[entryId] = true }

    /** How far the feed can be swiped: up to and including the first lesson whose question is still unanswered. */
    fun reachable(): Int {
        val hold = queue.indexOfFirst { it.item.lesson.questions > 0 && passed[it.id] != true }
        return if (hold < 0) queue.size else hold + 1
    }

    /** True when the lesson at [index] must be answered before the feed moves on. */
    fun holds(index: Int): Boolean = queue.getOrNull(index)?.let { it.item.lesson.questions > 0 && passed[it.id] != true } == true

    /** Feed positions the pager should scroll to. */
    val jumps = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private var lessons: List<FeedItem> = emptyList()
    private var currentIndex = 0
    private var nextId = 0L
    private var sinceNew = 0
    private val rated = HashSet<Long>()
    private var playJob: Job? = null
    private var autoSyncJob: Job? = null
    private var lastAutoAttempt = 0L

    /**
     * Lessons sent for a redo, with the version they had ([app.youtop.data.Lesson.rev]). They stay out
     * of the feed (new and review alike) until a newer version of the same lesson is synced; that
     * version then replaces the old one and starts as a new lesson.
     */
    private val redo = HashMap<String, Int>()

    init {
        viewModelScope.launch {
            val s = prefs.settings.first()
            narrator.setSpeed(s.speed)
            narrator.setMuted(s.muted)
            settings.first { it == s } // the scheduler reads its settings from this flow
            redo.putAll(s.redo)
            store.load()
            repo.loadLocal()
            loaded.value = true
            // Rebuild what lies ahead whenever the books or the set of books in the feed change.
            launch { repo.books.collect { lessons = it.flatten(); releaseRedone(); replan() } }
            // A changed pace or "when nothing is due" may allow another lesson right away.
            // (Books and topics are handled by the collector below, which rebuilds the pool first.)
            launch {
                settings.map { it.study.copy(excludedBooks = emptySet(), focus = emptySet()) }.distinctUntilChanged()
                    .collect { extendQueue(if (queue.isEmpty()) -1 else currentIndex) }
            }
            // The learner chose other books or topics: the feed starts again from what they chose.
            launch {
                prefs.settings.map { Triple(it.study.excludedBooks, it.study.focus, it.study.book) }.distinctUntilChanged().collect { (excluded, focus, book) ->
                    settings.first { s -> s.study.focus == focus && s.study.excludedBooks == excluded && s.study.book == book }
                    replan(restart = true)
                }
            }
        }
    }

    // ---- feed ------------------------------------------------------------

    private fun study() = settings.value.study

    /** Lessons the feed may draw from: the chosen books and tags. Rebuilt in [replan]. */
    private var pool: List<FeedItem> = emptyList()


    /** Lets back every held lesson whose newer version has arrived. */
    private fun releaseRedone() {
        if (redo.isEmpty()) return
        val back = Feedback.redone(redo, lessons)
        if (back.isEmpty()) return
        back.forEach { redo.remove(it); store.forget(it) }
        val now = HashMap(redo)
        viewModelScope.launch { prefs.setRedo(now) }
    }

    /** How many lessons are waiting for their redo. */
    val waitingForRedo: Int get() = settings.value.redo.size

    /** Shows every held lesson again, as it is (for a redo that was never sent). */
    fun bringBackRedo() {
        redo.clear()
        viewModelScope.launch { prefs.setRedo(emptyMap()) }
        replan()
    }
    private var topicOf: Map<String, String?> = emptyMap()

    /**
     * The pool in the order new lessons should be started: weakest topics first when that is on,
     * otherwise book order. Runs on every swipe, so it is one pass and one sort.
     */
    private fun candidates(): List<FeedItem> {
        if (!study().weakFirst) return pool
        val cards = progress.value.cards
        val reps = HashMap<String, Int>()
        val lapses = HashMap<String, Int>()
        for (item in pool) {
            val topic = topicOf[item.key] ?: continue
            val card = cards[item.key] ?: continue
            reps[topic] = (reps[topic] ?: 0) + card.reps
            lapses[topic] = (lapses[topic] ?: 0) + card.lapses
        }
        fun weakness(item: FeedItem): Float {
            val topic = topicOf[item.key] ?: return 0f
            val r = reps[topic] ?: 0
            return if (r >= 3) (lapses[topic] ?: 0).toFloat() / r else 0f
        }
        return pool.sortedByDescending(::weakness) // stable: book order within equally weak topics
    }

    suspend fun lesson(item: FeedItem): Lesson? = repo.lesson(item)

    /** Makes sure there is one entry beyond [index] for the pager to preload and swipe to. */
    private fun extendQueue(index: Int) {
        val items = candidates()
        val byKey = items.associateBy { it.key }
        val p = progress.value
        val now = System.currentTimeMillis()
        val dayEnd = Scheduler.dayEnd(now, java.util.TimeZone.getDefault().getOffset(now))
        val keys = items.map { it.key }
        while (queue.size < index + 2) {
            val pendingNew = queue.drop(index + 1).count { (p.cards[it.item.key]?.phase ?: Phase.NEW) == Phase.NEW }
            val key = Scheduler.next(
                keys = keys,
                cards = p.cards,
                recent = queue.takeLast(8).map { it.item.key },
                newToday = p.newToday + pendingNew,
                sinceNew = sinceNew,
                now = now,
                s = study(),
                dayEnd = dayEnd,
            )
            val item = key?.let(byKey::get)
            finishedForToday.value = item == null && items.isNotEmpty()
            if (item == null) break
            sinceNew = if ((p.cards[item.key]?.phase ?: Phase.NEW) == Phase.NEW) 0 else sinceNew + 1
            queue += FeedEntry(nextId++, item)
            preload(item)
        }
    }

    /** Fetches a coming lesson's picture and audio ahead of time, so swiping to it is instant. */
    private fun preload(item: FeedItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.lesson(item)
            (listOfNotNull(item.lesson.image) + item.lesson.audio + item.lesson.figures).forEach { runCatching { media.fetch(it) } }
        }
    }

    /** Keeps what has been shown, refreshes it with newly synced content, and plans afresh from there. */
    private fun replan(restart: Boolean = false) {
        val s = study()
        // One book when one is chosen (and still exists); otherwise every book not left out.
        val only = s.book?.takeIf { id -> lessons.any { it.book.id == id } }
        pool = lessons.filter { it.key !in redo && (if (only != null) it.book.id == only else it.book.id !in s.excludedBooks) && Tags.matches(it.lesson.tags, s.focus) }
        topicOf = pool.associate { it.key to Tags.mainTopic(it.lesson.tags) }
        if (pool.isEmpty()) {
            // Nothing matches the chosen books and topics: an empty feed that says so, rather than
            // the last lesson with nothing after it.
            queue.clear()
            currentIndex = 0
            narrator.unload()
            finishedForToday.value = false
            if (restart) choosing = false
            return
        }
        // After a new choice, a lesson on screen that is not part of it makes way at once:
        // "Study this book" must open on that book, not on whatever was showing before.
        val shown = queue.getOrNull(currentIndex)
        if (restart && shown != null && pool.none { it.key == shown.item.key }) {
            queue.clear()
            currentIndex = 0
            sinceNew = 0
            narrator.unload()
            generation++
        }
        val wasChoosing = choosing && restart
        if (restart) choosing = false
        val byKey = lessons.associateBy { it.key }
        while (queue.size > currentIndex + 1) queue.removeAt(queue.lastIndex)
        for (i in queue.indices) byKey[queue[i].item.key]?.let { if (it != queue[i].item) queue[i] = queue[i].copy(item = it) }
        extendQueue(if (queue.isEmpty()) -1 else currentIndex)
        // The lesson on screen stayed (it belongs to the new choice too) and the library is already closed.
        if (wasChoosing && shown != null && queue.getOrNull(currentIndex)?.id == shown.id) resume()
    }

    /** Studies one book (or all of them, with null) and goes on from there. */
    fun studyBook(book: String?) {
        if (book == study().book) return
        choosing = true
        narrator.pause()
        setStudy(study().copy(book = book))
    }

    /**
     * The library has closed: the lesson on screen plays on. Not while a new choice of book is
     * still on its way, not under an open question, and not with "question first".
     */
    fun resume() {
        if (covered || choosing || quizOpen) return
        val entry = queue.getOrNull(currentIndex) ?: return
        if (study().questionFirst && entry.item.lesson.questions > 0) return
        playJob?.cancel()
        playJob = viewModelScope.launch {
            val tracks = repo.lesson(entry.item)?.tracks.orEmpty()
            val index = (trackChoice[entry.id] ?: 0).coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
            val track = tracks.getOrNull(index) ?: return@launch
            narrator.load("${entry.id}#$index", track, media.audioUri(track.audio), autoPlay = false)
            if (!covered && !quizOpen && !narrator.state.value.ended) narrator.play()
        }
    }

    fun activate(index: Int) {
        currentIndex = index
        val entry = queue.getOrNull(index) ?: return narrator.unload()
        extendQueue(index)
        // With "question first" the lesson opens on its question and stays silent until asked.
        play(entry, autoPlay = !covered && !choosing && !(study().questionFirst && entry.item.lesson.questions > 0))
    }

    /** The app came back to the screen. */
    fun onForeground() {
        store.rollDay()
        if (loaded.value) extendQueue(if (queue.isEmpty()) -1 else currentIndex)
        startAutoSync()
    }

    /** The app left the screen. */
    fun onBackground() {
        stopAutoSync()
        narrator.pause()
        narrator.stopSaying()
        store.flush()
    }

    private fun play(entry: FeedEntry, autoPlay: Boolean = true) {
        playJob?.cancel()
        if (narrator.state.value.key?.startsWith("${entry.id}#") != true) narrator.unload()
        playJob = viewModelScope.launch {
            val tracks = repo.lesson(entry.item)?.tracks.orEmpty()
            val index = (trackChoice[entry.id] ?: 0).coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
            val track = tracks.getOrNull(index) ?: return@launch narrator.unload()
            narrator.load("${entry.id}#$index", track, media.audioUri(track.audio), autoPlay)
        }
    }

    fun selectTrack(entry: FeedEntry, index: Int) {
        trackChoice[entry.id] = index
        if (queue.getOrNull(currentIndex)?.id == entry.id) play(entry)
    }

    /**
     * Called when the learner has finished with a feed entry (answered its quiz, or moved on).
     * What they did is turned into a review rating; nothing is asked of them.
     */
    fun finish(entry: FeedEntry, answered: Int, wrong: Int, avgAnswerMs: Long, finishedNarration: Boolean, dwellMs: Long) {
        if (entry.id in rated) return
        val rating = Scheduler.rate(answered, wrong, entry.item.lesson.questions, avgAnswerMs, finishedNarration, dwellMs) ?: return
        rated += entry.id
        store.record(entry.item.key, rating, study())
        // The entry after this one was chosen before this result was known; choose again.
        if (queue.getOrNull(currentIndex)?.id == entry.id) {
            while (queue.size > currentIndex + 1) queue.removeAt(queue.lastIndex)
            extendQueue(currentIndex)
        }
    }

    /** Opens a lesson picked in the library as the next entry of the feed. */
    fun jumpTo(key: String) {
        val item = lessons.firstOrNull { it.key == key } ?: return
        val at = if (queue.isEmpty()) 0 else currentIndex + 1
        while (queue.size > at) queue.removeAt(queue.lastIndex)
        // A lesson picked by hand is opened even when the one on screen is still unanswered.
        queue.forEach { pass(it.id) }
        queue += FeedEntry(nextId++, item)
        preload(item)
        jumps.tryEmit(at)
    }

    fun setSpeed(speed: Float) {
        narrator.setSpeed(speed)
        viewModelScope.launch { prefs.setSpeed(speed) }
    }

    fun setMuted(muted: Boolean) {
        narrator.setMuted(muted)
        viewModelScope.launch { prefs.setMuted(muted) }
    }

    fun toggleSaved(key: String) = viewModelScope.launch { prefs.toggleSaved(key) }

    /** "Good job" on a lesson: kept, and sent to the agent with the next notes. Tapped again, taken back. */
    fun toggleLiked(item: FeedItem, title: String) = viewModelScope.launch {
        prefs.toggleLiked(item.key, Note(Feedback.GOOD, item.key, title, at = System.currentTimeMillis()))
    }

    fun addNote(note: Note) = viewModelScope.launch { prefs.addNote(note) }

    /**
     * Opens GitHub's "new issue" page with the waiting notes written in (the owner presses "Create"
     * there, and the agent reads it). The notes it holds are taken off the phone's list once the page
     * has opened. False when nothing waits or no browser could open it.
     */
    /**
     * Redo: keeps [note], takes the lesson out of the feed at once (it is not rated and does not come
     * back for review) until its redone version arrives, and opens GitHub with every waiting note.
     * False when GitHub could not be opened; the notes then wait on the phone.
     */
    fun submitRedo(entry: FeedEntry, note: Note, open: (String) -> Boolean): Boolean {
        val key = entry.item.key
        redo[key] = entry.item.lesson.rev
        val held = HashMap(redo)
        rated += entry.id
        queue.filter { it.item.key == key }.forEach { pass(it.id); rated += it.id }
        // What lies ahead was chosen with this lesson still in the pool: choose again without it.
        replan()
        // And the feed moves on to the next lesson by itself (none when nothing else is left).
        if (queue.size > currentIndex + 1) jumps.tryEmit(currentIndex + 1)
        val all = Feedback.add(settings.value.notes, note)
        val (url, sent) = Feedback.issue(all, BuildConfig.VERSION_NAME)!!
        val opened = open(url)
        viewModelScope.launch {
            prefs.setRedo(held)
            prefs.addNote(note)
            if (opened) prefs.sentNotes(sent)
        }
        return opened
    }

    fun sendNotes(open: (String) -> Boolean): Boolean {
        val (url, sent) = Feedback.issue(settings.value.notes, BuildConfig.VERSION_NAME) ?: return false
        if (!open(url)) return false
        viewModelScope.launch { prefs.sentNotes(sent) }
        return true
    }

    fun setStudy(study: StudySettings) = viewModelScope.launch { prefs.setStudy(study) }

    /** Back to every book and every topic, when the chosen ones leave the feed empty. */
    fun clearFilters() = setStudy(study().copy(focus = emptySet(), excludedBooks = emptySet(), book = null))

    fun hintSeen() = viewModelScope.launch { prefs.setHintSeen() }

    // ---- backup ------------------------------------------------------------

    /** Everything that cannot be downloaded again: the learning history, the saved lessons and the study settings. */
    fun backupText(): String = Backup.encode(Backup(made = System.currentTimeMillis(), progress = progress.value, study = study(), saved = settings.value.saved))

    /** Puts a backup back. Returns false, changing nothing, when [text] is not a YouTop backup. */
    suspend fun restoreBackup(text: String): Boolean {
        val backup = Backup.decode(text) ?: return false
        store.restore(backup.progress)
        store.flush()
        prefs.setSaved(backup.saved)
        backup.study?.let { prefs.setStudy(it) }
        rated.clear()
        replan()
        return true
    }

    fun resetProgress() {
        store.reset()
        rated.clear()
        replan()
    }

    // ---- sync ------------------------------------------------------------

    fun syncNow() = viewModelScope.launch {
        // Not before the lessons on the phone are read: sync decides what to keep from what it holds.
        loaded.first { it }
        val s = prefs.settings.first() // not settings.value: a link pasted a moment ago may not have reached it yet
        // Lessons come from GitHub; the Drive folder is only the way back when GitHub cannot be read.
        if (repo.sync(DriveClient.parseFolderId(s.driveFolder), s.offlineDownload)) prefs.setLastSync(System.currentTimeMillis())
    }

    /** Called while the app is in the foreground: refreshes on open and then periodically. */
    fun startAutoSync() {
        autoSyncJob?.cancel()
        // Screen tests must not depend on what is published today, or on the network.
        if (android.os.Build.FINGERPRINT == "robolectric") return
        autoSyncJob = viewModelScope.launch {
            while (true) {
                val s = prefs.settings.first()
                val now = System.currentTimeMillis()
                val due = now - maxOf(s.lastSync, lastAutoAttempt) > AUTO_SYNC_INTERVAL_MS
                if (s.autoSync && due) {
                    lastAutoAttempt = now
                    syncNow().join()
                }
                delay(60_000)
            }
        }
    }

    fun stopAutoSync() {
        autoSyncJob?.cancel()
    }

    fun setDriveFolder(link: String) = viewModelScope.launch { prefs.setDriveFolder(link.trim()) }
    fun setAutoSync(v: Boolean) = viewModelScope.launch { prefs.setAutoSync(v) }
    fun setOfflineDownload(v: Boolean) = viewModelScope.launch { prefs.setOfflineDownload(v) }

    fun clearMedia() = viewModelScope.launch {
        withContext(Dispatchers.IO) { media.clear() }
    }

    override fun onCleared() {
        store.flush()
        narrator.release()
    }

    private companion object {
        const val AUTO_SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
}
