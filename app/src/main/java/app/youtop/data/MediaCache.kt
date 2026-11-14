package app.youtop.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Persistent store for images and audio downloaded from Drive, so lessons work offline.
 * Kept out of Android's backup: the backup has a small size limit, and once pictures push it
 * over that limit the learning progress would silently stop being backed up too.
 */
class MediaCache(context: Context, private val drive: DriveClient) {
    private val dir = File(context.noBackupFilesDir, "media").apply { mkdirs() }
    private val locks = ConcurrentHashMap<String, Any>()

    init {
        // Where pictures were kept up to version 1.5.
        val old = File(context.filesDir, "media")
        if (old.exists()) thread { old.deleteRecursively() }
    }

    private fun driveId(ref: String) = ref.removePrefix("drive:").takeIf { ref.startsWith("drive:") }

    private fun file(id: String) = File(dir, id.replace(Regex("[^\\w-]"), "_"))

    fun isCached(ref: String): Boolean = driveId(ref)?.let { file(it).exists() } ?: true

    /** Something Coil can load: a local file when cached, otherwise a URL. */
    fun imageModel(ref: String?): Any? = when {
        ref == null -> null
        ref.startsWith("asset:") -> "file:///android_asset/" + ref.removePrefix("asset:")
        ref.startsWith("drive:") -> {
            val id = ref.removePrefix("drive:")
            file(id).takeIf { it.exists() } ?: DriveClient.downloadUrl(id)
        }
        else -> ref
    }

    /** Like [imageModel], but null while a Drive picture has not been downloaded yet. */
    fun cachedOrNull(ref: String?): Any? =
        if (ref != null && ref.startsWith("drive:") && !isCached(ref)) null else imageModel(ref)

    /**
     * Downloads the picture if needed (once, however many callers ask) and returns what to show.
     * The feed uses this instead of handing Coil the URL, which made it fetch every new picture
     * two or three times over mobile data.
     */
    suspend fun localImage(ref: String?): Any? = withContext(Dispatchers.IO) {
        if (ref != null) runCatching { fetch(ref) }
        imageModel(ref)
    }

    /** For small rows in lists: the stored picture when there is one, otherwise Drive's small thumbnail. */
    fun thumbModel(ref: String?): Any? {
        val id = ref?.let(::driveId) ?: return imageModel(ref)
        return file(id).takeIf { it.exists() } ?: "https://drive.google.com/thumbnail?id=$id&sz=w240"
    }

    fun audioUri(ref: String?): Uri? = when {
        ref == null -> null
        ref.startsWith("asset:") -> Uri.parse("asset:///" + ref.removePrefix("asset:"))
        ref.startsWith("drive:") -> {
            val id = ref.removePrefix("drive:")
            val f = file(id)
            if (f.exists()) Uri.fromFile(f) else Uri.parse(DriveClient.downloadUrl(id))
        }
        else -> Uri.parse(ref)
    }

    /** Blocking; call from a background dispatcher. Two callers asking for the same file wait for one download. */
    fun fetch(ref: String) {
        val id = driveId(ref) ?: return
        val f = file(id)
        if (f.exists()) return
        synchronized(locks.getOrPut(id) { Any() }) {
            if (!f.exists()) drive.download(id, f)
        }
    }

    /** Removes cached files that no current lesson refers to. Downloads in progress are left alone. */
    fun prune(keep: Collection<String>) {
        val names = keep.mapNotNull { driveId(it) }.map { file(it).name }.toSet()
        val stale = System.currentTimeMillis() - 60 * 60 * 1000L
        dir.listFiles()?.forEach {
            if (it.name.endsWith(".part")) { if (it.lastModified() < stale) it.delete() }
            else if (it.name !in names) it.delete()
        }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
}
