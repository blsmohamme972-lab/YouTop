package app.youtop.data

import android.content.Context
import android.net.Uri
import java.io.File

/** Persistent store for images and audio downloaded from Drive, so lessons work offline. */
class MediaCache(context: Context, private val drive: DriveClient) {
    private val dir = File(context.filesDir, "media").apply { mkdirs() }

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

    /** Blocking; call from a background dispatcher. */
    fun fetch(ref: String) {
        val id = driveId(ref) ?: return
        val f = file(id)
        if (!f.exists()) drive.download(id, f)
    }

    /** Removes cached files that no current lesson refers to. */
    fun prune(keep: Collection<String>) {
        val names = keep.mapNotNull { driveId(it) }.map { file(it).name }.toSet()
        dir.listFiles()?.forEach { if (it.name !in names) it.delete() }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
}
