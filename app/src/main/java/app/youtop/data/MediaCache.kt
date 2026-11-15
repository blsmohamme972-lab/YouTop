package app.youtop.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Persistent store for images and audio downloaded from GitHub (`gh:`) or Drive (`drive:`), so
 * lessons work offline.
 * Kept out of Android's backup: the backup has a small size limit, and once pictures push it
 * over that limit the learning progress would silently stop being backed up too.
 */
class MediaCache(context: Context, private val drive: DriveClient) {
    private val dir = File(context.noBackupFilesDir, "media").also { dir ->
        // Up to version 1.5 pictures lived in the backed-up files folder. Take them along
        // (same disk, so this is an instant rename) rather than download everything again.
        val old = File(context.filesDir, "media")
        if (old.isDirectory && !dir.exists()) old.renameTo(dir)
        dir.mkdirs()
        if (old.exists()) thread { old.deleteRecursively() }
    }
    private val locks = ConcurrentHashMap<String, Any>()

    /**
     * The name a downloadable reference is stored under, or null for `asset:` and plain URLs.
     * A Drive file keeps the name it had before 1.7 (its id), so nothing is downloaded twice.
     */
    private fun key(ref: String): String? = when {
        ref.startsWith(DriveClient.DRIVE) -> ref.removePrefix(DriveClient.DRIVE)
        ref.startsWith(DriveClient.GH) -> ref
        else -> null
    }

    private fun file(key: String) = File(dir, key.replace(Regex("[^\\w-]"), "_"))

    fun isCached(ref: String): Boolean = key(ref)?.let { file(it).exists() } ?: true

    /** Something Coil can load: a local file when cached, otherwise a URL. */
    fun imageModel(ref: String?): Any? = when {
        ref == null -> null
        ref.startsWith("asset:") -> "file:///android_asset/" + ref.removePrefix("asset:")
        else -> key(ref)?.let { k -> file(k).takeIf { it.exists() } ?: drive.url(ref) } ?: ref
    }

    /** Like [imageModel], but null while a remote picture has not been downloaded yet. */
    fun cachedOrNull(ref: String?): Any? =
        if (ref != null && !isCached(ref)) null else imageModel(ref)

    /**
     * Downloads the picture if needed (once, however many callers ask) and returns what to show.
     * The feed uses this instead of handing Coil the URL, which made it fetch every new picture
     * two or three times over mobile data.
     */
    suspend fun localImage(ref: String?): Any? = withContext(Dispatchers.IO) {
        if (ref != null) runCatching { fetch(ref) }
        imageModel(ref)
    }

    /**
     * For small rows in lists: the stored picture when there is one, otherwise Drive's small
     * thumbnail. GitHub has no thumbnails, so there it is the picture itself (about 300 KB).
     */
    fun thumbModel(ref: String?): Any? {
        val k = ref?.let(::key) ?: return imageModel(ref)
        val f = file(k)
        return when {
            f.exists() -> f
            ref.startsWith(DriveClient.DRIVE) -> "https://drive.google.com/thumbnail?id=$k&sz=w240"
            else -> drive.url(ref)
        }
    }

    fun audioUri(ref: String?): Uri? = when {
        ref == null -> null
        ref.startsWith("asset:") -> Uri.parse("asset:///" + ref.removePrefix("asset:"))
        else -> key(ref)?.let { k ->
            val f = file(k)
            if (f.exists()) Uri.fromFile(f) else Uri.parse(drive.url(ref))
        } ?: Uri.parse(ref)
    }

    /** Blocking; call from a background dispatcher. Two callers asking for the same file wait for one download. */
    fun fetch(ref: String) {
        val k = key(ref) ?: return
        val f = file(k)
        if (f.exists()) return
        synchronized(locks.getOrPut(k) { Any() }) {
            if (!f.exists()) drive.download(ref, f)
        }
    }

    /** Removes cached files that no current lesson refers to. Downloads in progress are left alone. */
    fun prune(keep: Collection<String>) {
        val names = keep.mapNotNull { key(it) }.map { file(it).name }.toSet()
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
