package app.youtop

import androidx.test.core.app.ApplicationProvider
import app.youtop.data.Book
import app.youtop.data.BookRef
import app.youtop.data.Catalog
import app.youtop.data.Chapter
import app.youtop.data.ChapterFile
import app.youtop.data.ContentJson
import app.youtop.data.ContentRepository
import app.youtop.data.DriveClient
import app.youtop.data.Lesson
import app.youtop.data.MediaCache
import app.youtop.data.Segment
import app.youtop.data.Track
import app.youtop.data.flatten
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Lessons are published on the `content` branch of the GitHub repository (`gh:<path>`); the
 * Drive folder is read only while GitHub cannot be. Both are pretended here; nothing is fetched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GitHubSyncTest {
    private val base = "https://raw.example/content/"
    private var gitHubUp = true
    private val asked = ArrayList<String>()

    private fun lesson(image: String, audio: String?) = Lesson(
        id = "q1", title = "Question 1", image = image,
        tracks = listOf(Track("story", "ar", audio = audio, segments = listOf(Segment("جملة", t = 0)))),
    )

    private val gitHub = mapOf(
        "catalog.json" to ContentJson.encodeToString(Catalog(listOf(BookRef("b", "gh:books/b/book-11111111.json")))),
        "books/b/book-11111111.json" to ContentJson.encodeToString(
            Book("b", "From GitHub", cover = "gh:books/b/media/q1.webp",
                chapters = listOf(Chapter("c", "Chapter", file = "gh:books/b/chapters/c-22222222.json")))
        ),
        "books/b/chapters/c-22222222.json" to ContentJson.encodeToString(
            ChapterFile(listOf(lesson("gh:books/b/media/q1.webp", "gh:books/b/media/q1-story-ar.m4a")))
        ),
        "books/b/media/q1.webp" to "picture",
        "books/b/media/q1-story-ar.m4a" to "sound",
    )
    private val driveFiles = mapOf(
        "catalog" to ContentJson.encodeToString(Catalog(listOf(BookRef("b", "bookid")))),
        "bookid" to ContentJson.encodeToString(
            Book("b", "From Drive", chapters = listOf(Chapter("c", "Chapter", lessons = listOf(lesson("drive:oldpicture", null)))))
        ),
        "oldpicture" to "old picture",
    )
    private val page = """<div class="flip-entry" id="entry-catalog"><a href="https://drive.google.com/file/d/catalog/view"><div class="flip-entry-title">catalog-6.json</div></a></div>"""

    private fun pretendNetwork() = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        val url = chain.request().url
        asked += url.toString()
        val body = when {
            url.toString().startsWith(base) -> if (gitHubUp) gitHub[url.toString().removePrefix(base)] else null
            url.host == "drive.google.com" -> page
            else -> driveFiles[url.queryParameter("id")]
        }
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(if (body == null) 404 else 200).message(if (body == null) "Not Found" else "OK")
            .body((body ?: "404: Not Found").toResponseBody("application/octet-stream".toMediaType())).build()
    }).build()

    @Test
    fun gitHubFirstThenDriveOnlyUntilGitHubContentIsHeld() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val drive = DriveClient(pretendNetwork(), contentBase = base.trimEnd('/'))
        val media = MediaCache(context, drive)
        val repo = ContentRepository(context, drive, media)
        fun title() = repo.books.value.first { it.id == "b" }.title

        // GitHub cannot be read and nothing from it is held yet: the Drive folder, as before 1.7.
        gitHubUp = false
        assertTrue(repo.sync("folder", downloadMedia = true))
        assertEquals("From Drive", title())
        assertTrue(media.isCached("drive:oldpicture"))
        val oldPicture = media.imageModel("drive:oldpicture") as File
        assertEquals("oldpicture", oldPicture.name) // the name used before 1.7, so nothing is fetched twice

        // GitHub answers: its content replaces Drive's, under the same book/chapter/lesson ids.
        gitHubUp = true
        asked.clear()
        assertTrue(repo.sync("folder", downloadMedia = true))
        assertEquals("From GitHub", title())
        assertTrue(asked.none { "google" in it })
        val item = repo.books.value.flatten().single { it.book.id == "b" }
        assertEquals("b/c/q1", item.key)
        assertEquals("gh:books/b/media/q1-story-ar.m4a", repo.lesson(item)!!.tracks[0].audio)
        assertEquals("picture", (media.imageModel("gh:books/b/media/q1.webp") as File).readText())
        assertEquals("picture", (media.thumbModel("gh:books/b/media/q1.webp") as File).readText())
        assertEquals("file", media.audioUri("gh:books/b/media/q1-story-ar.m4a")!!.scheme)
        assertFalse(oldPicture.exists()) // pruned: no lesson refers to it any more

        // A second sync fetches the catalog again and nothing else.
        asked.clear()
        assertTrue(repo.sync("folder", downloadMedia = true))
        assertEquals(listOf(base + "catalog.json"), asked)

        // GitHub down again: keep what is held rather than go back to the old Drive copy.
        gitHubUp = false
        assertFalse(repo.sync("folder", downloadMedia = true))
        assertEquals("From GitHub", title())
        assertTrue(media.isCached("gh:books/b/media/q1.webp"))

        // Not downloaded yet: the address on GitHub.
        media.clear()
        assertEquals(base + "books/b/media/q1.webp", media.imageModel("gh:books/b/media/q1.webp"))
        assertEquals(null, media.cachedOrNull("gh:books/b/media/q1.webp"))
        assertEquals(base + "books/b/media/q1-story-ar.m4a", media.audioUri("gh:books/b/media/q1-story-ar.m4a").toString())
    }
}
