package app.youtop

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.youtop.data.ContentRepository
import app.youtop.data.DriveClient
import app.youtop.data.FeedItem
import app.youtop.data.MediaCache
import app.youtop.data.Prefs
import app.youtop.data.Settings
import app.youtop.data.flatten
import app.youtop.player.Narrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val drive = DriveClient(http, BuildConfig.DRIVE_API_KEY)
    private val prefs = Prefs(app)

    val media = MediaCache(app, drive)
    private val repo = ContentRepository(app, drive, media)
    val narrator = Narrator(app, viewModelScope)

    val books = repo.books
    val sync = repo.sync
    val feed: StateFlow<List<FeedItem>> =
        repo.books.map { it.flatten() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** False until the bundled and cached lessons have been read from disk. */
    val loaded = MutableStateFlow(false)

    /** Which narration (story / explanation) is selected for each lesson. */
    val trackChoice = mutableStateMapOf<String, Int>()

    /** Lesson keys the feed should scroll to. */
    val jumps = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private var current: FeedItem? = null
    private var autoSyncJob: Job? = null
    private var lastAutoAttempt = 0L

    init {
        viewModelScope.launch {
            val s = prefs.settings.first()
            narrator.setSpeed(s.speed)
            narrator.setMuted(s.muted)
            repo.loadLocal()
            loaded.value = true
        }
    }

    // ---- narration -------------------------------------------------------

    fun activate(item: FeedItem?) {
        current = item
        if (item == null) return narrator.unload()
        val index = (trackChoice[item.key] ?: 0).coerceIn(0, (item.lesson.tracks.size - 1).coerceAtLeast(0))
        val track = item.lesson.tracks.getOrNull(index) ?: return narrator.unload()
        narrator.load("${item.key}#$index", track, media.audioUri(track.audio))
    }

    fun selectTrack(item: FeedItem, index: Int) {
        trackChoice[item.key] = index
        if (current?.key == item.key) activate(item)
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

    fun jumpTo(key: String) {
        jumps.tryEmit(key)
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
