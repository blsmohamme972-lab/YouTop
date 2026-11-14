package app.youtop.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val done: Int = 0, val total: Int = 0) : SyncState
    data class Done(val books: Int) : SyncState
    data class Failed(val offline: Boolean, val message: String) : SyncState
}

/**
 * Lessons come from two places: the starter book bundled in the APK and the
 * books last synced from Google Drive (kept on disk so they work offline).
 */
class ContentRepository(
    private val context: Context,
    private val drive: DriveClient,
    private val media: MediaCache,
) {
    private val dir = File(context.filesDir, "content").apply { mkdirs() }
    private val booksDir = File(dir, "books").apply { mkdirs() }
    private val catalogFile = File(dir, "catalog.json")
    private val mutex = Mutex()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _sync = MutableStateFlow<SyncState>(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync

    suspend fun loadLocal() = withContext(Dispatchers.IO) {
        val synced = runCatching {
            ContentJson.decodeFromString<Catalog>(catalogFile.readText()).books.mapNotNull { ref ->
                runCatching { ContentJson.decodeFromString<Book>(bookFile(ref).readText()) }.getOrNull()
            }
        }.getOrDefault(emptyList())
        val ids = synced.map { it.id }.toSet()
        _books.value = synced + seedBooks().filter { it.id !in ids }
    }

    private fun seedBooks(): List<Book> =
        context.assets.list("seed").orEmpty().filter { it.endsWith(".json") }.sorted().mapNotNull { name ->
            runCatching {
                ContentJson.decodeFromString<Book>(context.assets.open("seed/$name").bufferedReader().use { it.readText() })
            }.getOrNull()
        }

    private fun bookFile(ref: BookRef) = File(booksDir, ref.file.replace(Regex("[^\\w-]"), "_") + ".json")

    /** Returns true when the sync finished. Safe to call concurrently; a second call waits for the first. */
    suspend fun sync(folderId: String, downloadMedia: Boolean): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            _sync.value = SyncState.Running()
            try {
                val catalogEntry = DriveClient.pickCatalog(drive.list(folderId))
                    ?: throw IllegalStateException("No catalog file in the Drive folder yet")
                val catalogText = drive.text(catalogEntry.id)
                val catalog = ContentJson.decodeFromString<Catalog>(catalogText)

                val books = catalog.books.map { ref ->
                    val f = bookFile(ref)
                    if (!f.exists()) {
                        val text = drive.text(ref.file)
                        ContentJson.decodeFromString<Book>(text) // validate before storing
                        File(booksDir, f.name + ".part").apply { writeText(text) }.renameTo(f)
                    }
                    ContentJson.decodeFromString<Book>(f.readText())
                }

                File(dir, "catalog.json.part").apply { writeText(catalogText) }.renameTo(catalogFile)
                val keep = catalog.books.map { bookFile(it).name }.toSet()
                booksDir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
                loadLocal()

                val refs = books.flatMap { it.mediaRefs() }.distinct()
                media.prune(refs)
                if (downloadMedia) {
                    val missing = refs.filterNot { media.isCached(it) }
                    missing.forEachIndexed { i, ref ->
                        _sync.value = SyncState.Running(i, missing.size)
                        media.fetch(ref)
                    }
                }
                _sync.value = SyncState.Done(books.size)
                true
            } catch (e: kotlinx.coroutines.CancellationException) {
                _sync.value = SyncState.Idle
                throw e
            } catch (e: IOException) {
                _sync.value = SyncState.Failed(offline = true, message = e.message.orEmpty())
                false
            } catch (e: Exception) {
                _sync.value = SyncState.Failed(offline = false, message = e.message ?: e.javaClass.simpleName)
                false
            }
        }
    }
}
