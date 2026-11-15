package app.youtop.data

import app.youtop.study.Progress
import app.youtop.study.StudySettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A file the learner can keep outside the phone (Drive, e-mail) and put back after a new phone or
 * a reinstall. Lessons are not in it; they are downloaded again.
 */
@Serializable
data class Backup(
    val app: String = "",
    val version: Int = 1,
    val made: Long = 0,
    val progress: Progress = Progress(),
    val study: StudySettings? = null,
    val saved: Set<String> = emptySet(),
) {
    companion object {
        const val APP = "youtop"
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        fun encode(b: Backup): String = json.encodeToString(b.copy(app = APP))

        /** Null when [text] is not a backup made by this app. */
        fun decode(text: String): Backup? =
            runCatching { json.decodeFromString<Backup>(text) }.getOrNull()?.takeIf { it.app == APP }
    }
}
