package app.youtop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.youtop.BuildConfig
import app.youtop.study.StudySettings
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("youtop")

data class Settings(
    val driveFolder: String = BuildConfig.DEFAULT_DRIVE_FOLDER,
    val autoSync: Boolean = true,
    val offlineDownload: Boolean = false,
    val speed: Float = 1f,
    val muted: Boolean = false,
    val lastSync: Long = 0L,
    val saved: Set<String> = emptySet(),
    val study: StudySettings = StudySettings(),
    /** The "swipe up for the next lesson" hint has been shown and acted on. */
    val hintSeen: Boolean = false,
    /** Lessons the owner said "good job" to (kept after sending, so the button stays lit). */
    val liked: Set<String> = emptySet(),
    /** Notes for the agent not sent yet (see [Feedback]). */
    val notes: List<Note> = emptyList(),
)

class Prefs(private val context: Context) {
    private val folder = stringPreferencesKey("drive_folder")
    private val autoSync = booleanPreferencesKey("auto_sync")
    private val offline = booleanPreferencesKey("offline_download")
    private val speed = floatPreferencesKey("speed")
    private val muted = booleanPreferencesKey("muted")
    private val lastSync = longPreferencesKey("last_sync")
    private val saved = stringSetPreferencesKey("saved")
    private val study = stringPreferencesKey("study")
    private val hintSeen = booleanPreferencesKey("hint_seen")
    private val liked = stringSetPreferencesKey("liked")
    private val notes = stringPreferencesKey("feedback_notes")

    val settings: Flow<Settings> = context.store.data.map { p ->
        Settings(
            driveFolder = p[folder] ?: BuildConfig.DEFAULT_DRIVE_FOLDER,
            autoSync = p[autoSync] ?: true,
            offlineDownload = p[offline] ?: false,
            speed = p[speed] ?: 1f,
            muted = p[muted] ?: false,
            lastSync = p[lastSync] ?: 0L,
            saved = p[saved] ?: emptySet(),
            study = p[study]?.let { runCatching { ContentJson.decodeFromString<StudySettings>(it) }.getOrNull() } ?: StudySettings(),
            hintSeen = p[hintSeen] ?: false,
            liked = p[liked] ?: emptySet(),
            notes = p[notes]?.let { runCatching { ContentJson.decodeFromString<List<Note>>(it) }.getOrNull() } ?: emptyList(),
        )
    }

    suspend fun setDriveFolder(v: String) = context.store.edit { it[folder] = v; it[lastSync] = 0L }
    suspend fun setAutoSync(v: Boolean) = context.store.edit { it[autoSync] = v }
    suspend fun setOfflineDownload(v: Boolean) = context.store.edit { it[offline] = v }
    suspend fun setSpeed(v: Float) = context.store.edit { it[speed] = v }
    suspend fun setMuted(v: Boolean) = context.store.edit { it[muted] = v }
    suspend fun setLastSync(v: Long) = context.store.edit { it[lastSync] = v }
    suspend fun setStudy(v: StudySettings) = context.store.edit { it[study] = ContentJson.encodeToString(v) }
    suspend fun setHintSeen() = context.store.edit { it[hintSeen] = true }
    suspend fun setSaved(v: Set<String>) = context.store.edit { it[saved] = v }
    /** "Good job" on [key], or taken back; a like not sent yet is taken back with it. */
    suspend fun toggleLiked(key: String, note: Note) = context.store.edit {
        val cur = it[liked] ?: emptySet()
        val waiting = readNotes(it[notes])
        if (key in cur) {
            it[liked] = cur - key
            it[notes] = ContentJson.encodeToString(waiting.filterNot { n -> n.key == key && n.kind == Feedback.GOOD })
        } else {
            it[liked] = cur + key
            it[notes] = ContentJson.encodeToString(Feedback.add(waiting, note))
        }
    }
    suspend fun addNote(note: Note) = context.store.edit {
        it[notes] = ContentJson.encodeToString(Feedback.add(readNotes(it[notes]), note))
    }
    /** Forgets the notes that were handed to GitHub. */
    suspend fun sentNotes(sent: List<Note>) = context.store.edit {
        it[notes] = ContentJson.encodeToString(readNotes(it[notes]).filterNot { n -> n in sent })
    }
    private fun readNotes(json: String?): List<Note> =
        json?.let { runCatching { ContentJson.decodeFromString<List<Note>>(it) }.getOrNull() } ?: emptyList()

    suspend fun toggleSaved(key: String) = context.store.edit {
        val cur = it[saved] ?: emptySet()
        it[saved] = if (key in cur) cur - key else cur + key
    }
}
