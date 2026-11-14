package app.youtop.study

import android.content.Context
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
import java.util.TimeZone

@Serializable
data class Progress(
    val cards: Map<String, CardState> = emptyMap(),
    val day: Long = 0,
    val doneToday: Int = 0,
    val newToday: Int = 0,
    val streak: Int = 0,
    val lastStudyDay: Long = 0,
) {
    /** Streak as it stands today: broken if a whole day was missed. */
    fun liveStreak(today: Long) = if (today - lastStudyDay <= 1) streak else 0
}

/** Keeps every lesson's memory state and the daily counters on disk. */
class ProgressStore(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "progress.json")
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    fun today(now: Long = System.currentTimeMillis()) = Scheduler.dayOf(now, TimeZone.getDefault().getOffset(now))

    suspend fun load() {
        val stored = withContext(Dispatchers.IO) {
            runCatching { json.decodeFromString<Progress>(file.readText()) }.getOrDefault(Progress())
        }
        _progress.value = stored.rolledTo(today())
    }

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
            )
        }
        save()
    }

    private fun max1(v: Int) = if (v < 1) 1 else v

    fun reset() {
        _progress.value = Progress(day = today())
        save()
    }

    private fun save() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            val text = json.encodeToString(_progress.value)
            withContext(Dispatchers.IO) {
                val tmp = File(file.parentFile, file.name + ".part")
                tmp.writeText(text)
                tmp.renameTo(file)
            }
        }
    }
}
