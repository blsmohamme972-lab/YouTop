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
        )
    }

    suspend fun setDriveFolder(v: String) = context.store.edit { it[folder] = v; it[lastSync] = 0L }
    suspend fun setAutoSync(v: Boolean) = context.store.edit { it[autoSync] = v }
    suspend fun setOfflineDownload(v: Boolean) = context.store.edit { it[offline] = v }
    suspend fun setSpeed(v: Float) = context.store.edit { it[speed] = v }
    suspend fun setMuted(v: Boolean) = context.store.edit { it[muted] = v }
    suspend fun setLastSync(v: Long) = context.store.edit { it[lastSync] = v }
    suspend fun setStudy(v: StudySettings) = context.store.edit { it[study] = ContentJson.encodeToString(v) }
    suspend fun toggleSaved(key: String) = context.store.edit {
        val cur = it[saved] ?: emptySet()
        it[saved] = if (key in cur) cur - key else cur + key
    }
}
