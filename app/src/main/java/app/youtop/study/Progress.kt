package app.youtop.study

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.TimeZone

@Serializable
data class Progress(
    val cards: Map<String, CardState> = emptyMap(),
    val day: Long = 0,
    val doneToday: Int = 0,
    val newToday: Int = 0,
    val streak: Int = 0,
    val lastStudyDay: Long = 0,
    /** What was done on each recent day (by day number, see [Scheduler.dayOf]); feeds the dashboard. */
    val history: Map<Long, DayLog> = emptyMap(),
) {
    /** Streak as it stands today: broken if a whole day was missed. */
    fun liveStreak(today: Long) = if (today - lastStudyDay <= 1) streak else 0
}

/** One day of study: lessons rated, and how many of them were answered wrongly. */
@Serializable
data class DayLog(val done: Int = 0, val again: Int = 0)

/** Keeps every lesson's memory state and the daily counters on disk. */
class ProgressStore(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "progress.json")
    // Written through AtomicFile: a crash or a dead battery in the middle of a save leaves the
    // previous file in place instead of an empty one.
    private val atomic = AtomicFile(file)
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null
    private val writeLock = Any()

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    fun today(now: Long = System.currentTimeMillis()) = Scheduler.dayOf(now, TimeZone.getDefault().getOffset(now))

    suspend fun load() {
        val stored = withContext(Dispatchers.IO) {
            val text = try {
                String(atomic.readFully(), Charsets.UTF_8)
            } catch (e: FileNotFoundException) {
                return@withContext Progress() // nothing saved yet
            } catch (e: IOException) {
                keepAside()
                return@withContext Progress()
            }
            runCatching { json.decodeFromString<Progress>(text) }.getOrElse {
                keepAside()
                Progress()
            }
        }
        _progress.value = stored.rolledTo(today())
    }

    /**
     * The saved history could not be read. The next rating will overwrite it, so put a copy next
     * to it first (one file, replaced each time, so it cannot pile up): never lose it silently.
     */
    private fun keepAside() {
        runCatching { if (file.exists()) file.copyTo(File(file.parentFile, "progress.unreadable.json"), overwrite = true) }
    }

    /** A new day may have begun while the app sat in memory: reset the daily counters. */
    fun rollDay() = _progress.update { it.rolledTo(today()) }

    private fun Progress.rolledTo(today: Long) = if (day == today) this else copy(day = today, doneToday = 0, newToday = 0)

    /** Records one rated lesson. */
    fun record(key: String, rating: Rating, settings: StudySettings, now: Long = System.currentTimeMillis()) {
        val today = today(now)
        _progress.update { old ->
            val p = old.rolledTo(today)
            val before = p.cards[key] ?: CardState()
            val fuzz = 0.95f + (key.hashCode() + before.reps).mod(11) / 100f
            p.copy(
                cards = p.cards + (key to Scheduler.review(before, rating, now, settings, fuzz)),
                doneToday = p.doneToday + 1,
                newToday = p.newToday + if (before.phase == Phase.NEW) 1 else 0,
                streak = when (today - p.lastStudyDay) {
                    0L -> max1(p.streak)
                    1L -> p.streak + 1
                    else -> 1
                },
                lastStudyDay = today,
                history = (p.history[today] ?: DayLog()).let { log ->
                    val kept = if (p.history.size > HISTORY_DAYS) p.history.filterKeys { it > today - HISTORY_DAYS } else p.history
                    kept + (today to DayLog(log.done + 1, log.again + if (rating == Rating.AGAIN) 1 else 0))
                },
            )
        }
        save()
    }

    private fun max1(v: Int) = if (v < 1) 1 else v

    /** Replaces everything with a restored backup. */
    fun restore(p: Progress) {
        _progress.value = p.rolledTo(today())
        save()
    }

    /** Forgets one lesson: it comes back as a new lesson (used when a redone lesson replaces the old one). */
    fun forget(key: String) {
        if (key !in _progress.value.cards) return
        _progress.update { it.copy(cards = it.cards - key) }
        save()
    }

    fun reset() {
        _progress.value = Progress(day = today())
        save()
    }

    private fun save() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            val snapshot = _progress.value
            withContext(Dispatchers.IO) { write(snapshot) }
        }
    }

    /** Writes a pending save at once; called when the app leaves the screen or is closed. */
    fun flush() {
        if (saveJob?.isActive != true) return
        saveJob?.cancel()
        // On the caller's thread on purpose: Android may end the process right after the app
        // leaves the screen, and a background thread might not get to finish.
        write(_progress.value)
    }

    private companion object {
        const val HISTORY_DAYS = 400
    }

    private fun write(p: Progress) = synchronized(writeLock) {
        val bytes = json.encodeToString(p).toByteArray(Charsets.UTF_8)
        val out = try { atomic.startWrite() } catch (e: IOException) { return@synchronized }
        try {
            out.write(bytes)
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
        }
    }
}
