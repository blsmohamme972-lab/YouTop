package app.youtop

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.youtop.data.ContentRepository
import app.youtop.data.DriveClient
import app.youtop.data.FeedItem
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
    private val drive = DriveClient(http, BuildConfig.DRIVE_API_KEY)
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

    init {
        viewModelScope.launch {
            val s = prefs.settings.first()
            narrator.setSpeed(s.speed)
            narrator.setMuted(s.muted)
            settings.first { it == s } // the scheduler reads its settings from this flow
            store.load()
            repo.loadLocal()
            loaded.value = true
            // Rebuild what lies ahead whenever the books or the set of books in the feed change.
            launch { repo.books.collect { lessons = it.flatten(); replan() } }
            launch { prefs.settings.map { it.study.excludedBooks to it.study.focus }.distinctUntilChanged().collect { settings.first { s -> s.study.focus == it.second && s.study.excludedBooks == it.first }; replan() } }
        }
    }

    // ---- feed ------------------------------------------------------------

    private fun study() = settings.value.study

    /** Lessons the feed may draw from: the chosen books and tags. Rebuilt in [replan]. */
    private var pool: List<FeedItem> = emptyList()
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
        while (queue.size < index + 2) {
            val pendingNew = queue.drop(index + 1).count { (p.cards[it.item.key]?.phase ?: Phase.NEW) == Phase.NEW }
            val key = Scheduler.next(
                keys = items.map { it.key },
                cards = p.cards,
                recent = queue.takeLast(8).map { it.item.key },
                newToday = p.newToday + pendingNew,
                sinceNew = sinceNew,
                now = System.currentTimeMillis(),
                s = study(),
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
            (listOfNotNull(item.lesson.image) + item.lesson.audio).forEach { runCatching { media.fetch(it) } }
        }
    }

    /** Keeps what has been shown, refreshes it with newly synced content, and plans afresh from there. */
    private fun replan() {
        val s = study()
        pool = lessons.filter { it.book.id !in s.excludedBooks && Tags.matches(it.lesson.tags, s.focus) }
        topicOf = pool.associate { it.key to Tags.mainTopic(it.lesson.tags) }
        val byKey = lessons.associateBy { it.key }
        while (queue.size > currentIndex + 1) queue.removeAt(queue.lastIndex)
        for (i in queue.indices) byKey[queue[i].item.key]?.let { if (it != queue[i].item) queue[i] = queue[i].copy(item = it) }
        extendQueue(if (queue.isEmpty()) -1 else currentIndex)
    }

    fun activate(index: Int) {
        currentIndex = index
        val entry = queue.getOrNull(index) ?: return narrator.unload()
        extendQueue(index)
        // With "question first" the lesson opens on its question and stays silent until asked.
        play(entry, autoPlay = !(study().questionFirst && entry.item.lesson.questions > 0))
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

    fun setStudy(study: StudySettings) = viewModelScope.launch { prefs.setStudy(study) }

    fun resetProgress() {
        store.reset()
        rated.clear()
        replan()
    }

    // ---- sync ------------------------------------------------------------

    fun syncNow() = viewModelScope.launch {
        val s = settings.value
        val folder = DriveClient.parseFolderId(s.driveFolder) ?: return@launch
        if (repo.sync(folder, s.offlineDownload)) prefs.setLastSync(System.currentTimeMillis())
    }

    /** Called while the app is in the foreground: refreshes on open and then periodically. */
    fun startAutoSync() {
        autoSyncJob?.cancel()
        // Screen tests must not depend on what is published in Drive today.
        if (android.os.Build.FINGERPRINT == "robolectric") return
        autoSyncJob = viewModelScope.launch {
            while (true) {
                val s = prefs.settings.first()
                val now = System.currentTimeMillis()
                val due = now - maxOf(s.lastSync, lastAutoAttempt) > AUTO_SYNC_INTERVAL_MS
                if (s.autoSync && due && DriveClient.parseFolderId(s.driveFolder) != null) {
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
        narrator.release()
    }

    private companion object {
        const val AUTO_SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
}
