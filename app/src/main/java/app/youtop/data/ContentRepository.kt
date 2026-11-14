package app.youtop.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val done: Int = 0, val total: Int = 0) : SyncState
    data class Done(val books: Int) : SyncState
    data class Failed(val offline: Boolean, val message: String) : SyncState
}

/**
 * Lessons come from two places: the starter book bundled in the APK and the books last synced
 * from Google Drive (kept on disk so they work offline).
 *
 * Only a light index of all lessons is held in memory ([books]); the text of a lesson is read
 * from its chapter file on demand ([lesson]), so thousands of lessons stay cheap.
 */
class ContentRepository(
    private val context: Context,
    private val drive: DriveClient,
    private val media: MediaCache,
) {
    private val dir = File(context.filesDir, "content").apply { mkdirs() }
    private val filesDir = File(dir, "files").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val taxonomyFile = File(dir, "taxonomy.json")
    private val mutex = Mutex()

    private val _books = MutableStateFlow<List<BookInfo>>(emptyList())
    val books: StateFlow<List<BookInfo>> = _books

    private val _taxonomy = MutableStateFlow(Taxonomy())
    val taxonomy: StateFlow<Taxonomy> = _taxonomy

    private val _sync = MutableStateFlow<SyncState>(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync

    suspend fun loadLocal() = withContext(Dispatchers.IO) {
        val synced = runCatching { ContentJson.decodeFromString<LibraryIndex>(indexFile.readText()).books }
            .getOrDefault(emptyList())
        val ids = synced.map { it.id }.toSet()
        _books.value = synced + seedBooks().filter { it.id !in ids }
        _taxonomy.value = runCatching { ContentJson.decodeFromString<Taxonomy>(taxonomyFile.readText()) }
            .recoverCatching { ContentJson.decodeFromString<Taxonomy>(assetText("taxonomy.json")) }
            .getOrDefault(Taxonomy())
    }

    private fun assetText(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    private fun seedBooks(): List<BookInfo> =
        context.assets.list("seed").orEmpty().filter { it.endsWith(".json") }.sorted().mapNotNull { name ->
            runCatching {
                val book = ContentJson.decodeFromString<Book>(assetText("seed/$name"))
                book.info { chapter -> "asset:seed/$name" to chapter.lessons }
            }.getOrNull()
        }

    private fun Book.info(lessonsOf: (Chapter) -> Pair<String, List<Lesson>>) = BookInfo(
        id, title, titleAr, description, cover,
        chapters.map { c ->
            val (source, lessons) = lessonsOf(c)
            ChapterInfo(c.id, c.title, c.titleAr, source, lessons.map { it.info() })
        },
    )

    private fun stored(fileId: String) = File(filesDir, fileId.replace(Regex("[^\\w-]"), "_") + ".json")

    /** Downloads a Drive JSON file unless it is already stored, and returns its text. */
    private fun fetch(fileId: String): String {
        val f = stored(fileId)
        if (!f.exists()) {
            val text = drive.text(fileId)
            File(filesDir, f.name + ".part").apply { writeText(text) }.renameTo(f)
        }
        return f.readText()
    }

    // ---- full lessons, read when needed ------------------------------------

    private val lessonCache = object : LinkedHashMap<String, Map<String, Lesson>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, Lesson>>?) = size > 6
    }

    suspend fun lesson(item: FeedItem): Lesson? = withContext(Dispatchers.IO) {
        val cacheKey = item.chapter.source + "#" + item.chapter.id
        val lessons = synchronized(lessonCache) { lessonCache[cacheKey] } ?: runCatching {
            val source = item.chapter.source
            val list = when {
                source.startsWith("chapter:") ->
                    ContentJson.decodeFromString<ChapterFile>(stored(source.removePrefix("chapter:")).readText()).lessons
                else -> {
                    val text = if (source.startsWith("asset:")) assetText(source.removePrefix("asset:"))
                    else stored(source.removePrefix("book:")).readText()
                    ContentJson.decodeFromString<Book>(text).chapters.first { it.id == item.chapter.id }.lessons
                }
            }
            list.associateBy { it.id }.also { synchronized(lessonCache) { lessonCache[cacheKey] = it } }
        }.getOrNull()
        lessons?.get(item.lesson.id)
    }

    // ---- sync ----------------------------------------------------------------

    /** Returns true when the sync finished. Safe to call concurrently; a second call waits for the first. */
    suspend fun sync(folderId: String, downloadMedia: Boolean): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            _sync.value = SyncState.Running()
            try {
                val catalogEntry = DriveClient.pickCatalog(drive.list(folderId))
                    ?: throw IllegalStateException("No catalog file in the Drive folder yet")
                val catalog = ContentJson.decodeFromString<Catalog>(drive.text(catalogEntry.id))

                val used = HashSet<String>()
                val parsed = catalog.books.map { ref ->
                    used += stored(ref.file).name
                    ref to ContentJson.decodeFromString<Book>(fetch(ref.file))
                }
                val chapterFiles = parsed.sumOf { (_, book) -> book.chapters.count { it.file != null } }
                var done = 0
                val books = parsed.map { (ref, book) ->
                    book.info { chapter ->
                        val file = chapter.file
                        if (file == null) {
                            "book:${ref.file}" to chapter.lessons
                        } else {
                            _sync.value = SyncState.Running(done++, chapterFiles)
                            used += stored(file).name
                            "chapter:$file" to ContentJson.decodeFromString<ChapterFile>(fetch(file)).lessons
                        }
                    }
                }
                catalog.taxonomy?.let { id ->
                    val text = drive.text(id)
                    ContentJson.decodeFromString<Taxonomy>(text) // validate before storing
                    taxonomyFile.writeText(text)
                }

                // Everything is on disk: switch over, then drop what is no longer referenced.
                File(dir, "index.json.part").apply { writeText(ContentJson.encodeToString(LibraryIndex(books))) }.renameTo(indexFile)
                filesDir.listFiles()?.forEach { if (it.name !in used) it.delete() }
                synchronized(lessonCache) { lessonCache.clear() }
                loadLocal()

                val refs = books.flatMap { it.mediaRefs() }.distinct()
                media.prune(refs)
                if (downloadMedia) {
                    val missing = refs.filterNot { media.isCached(it) }
                    missing.forEachIndexed { i, ref ->
                        _sync.value = SyncState.Running(i, missing.size)
                        runCatching { media.fetch(ref) }
                    }
                }
                _sync.value = SyncState.Done(books.size)
                true
            } catch (e: CancellationException) {
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
