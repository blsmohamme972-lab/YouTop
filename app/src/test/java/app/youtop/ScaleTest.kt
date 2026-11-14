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
import app.youtop.data.Question
import app.youtop.data.Segment
import app.youtop.data.Spot
import app.youtop.data.SyncState
import app.youtop.data.Track
import app.youtop.data.flatten
import app.youtop.study.CardState
import app.youtop.study.Phase
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import app.youtop.study.Tags
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The library is expected to grow past 5,000 lessons. This syncs a made-up book of that size
 * from a pretend Drive and checks that everything the app does per swipe stays fast.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScaleTest {
    private val chapters = 50
    private val perChapter = 110
    private val topics = listOf("cardiac/valve/aortic/stenosis", "cardiac/coronary/conduits", "thoracic/lung-cancer/staging", "vascular/aorta/aaa/evar")

    private fun lesson(c: Int, i: Int) = Lesson(
        id = "q$c-$i", title = "Question $c.$i", image = "drive:img-$c-$i",
        tags = listOf(topics[(c + i) % topics.size], "d:indications", "q:vignette", "l:${1 + i % 3}"),
        tracks = listOf(
            Track("story", "ar", segments = List(8) { Segment("جملة من القصة رقم $it ".repeat(6), spot = Spot(0.5f, 0.5f)) }),
            Track("explanation", "en", segments = List(7) { Segment("A sentence of explanation number $it. ".repeat(4)) }),
        ),
        quiz = listOf(Question("Stem of question $c.$i?", listOf("A", "B", "C", "D", "E"), i % 5, "Because.", source = "p. $c, Q$i")),
    )

    /** Answers the two URLs the app uses: the public folder page and file downloads. */
    private fun pretendDrive(): OkHttpClient {
        val files = HashMap<String, String>()
        val book = Book(
            "big", "Big MCQ book",
            chapters = List(chapters) { c ->
                files["ch$c"] = ContentJson.encodeToString(ChapterFile(List(perChapter) { lesson(c, it) }))
                Chapter("c$c", "Chapter $c", file = "ch$c")
            },
        )
        files["book"] = ContentJson.encodeToString(book)
        files["catalog"] = ContentJson.encodeToString(Catalog(listOf(BookRef("big", "book"))))
        val page = """<div class="flip-entry" id="entry-catalog"><a href="https://drive.google.com/file/d/catalog/view"><div class="flip-entry-title">catalog-1.json</div></a></div>"""
        return OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            val body = if (url.host == "drive.google.com") page else files.getValue(url.queryParameter("id")!!)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }).build()
    }

    private inline fun <T> timed(label: String, limitMs: Long, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val ms = (System.nanoTime() - start) / 1_000_000
        println("ScaleTest: $label took $ms ms")
        assertTrue("$label took $ms ms (limit $limitMs)", ms <= limitMs)
        return result
    }

    @Test
    fun fiveThousandLessonsStayFast() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val drive = DriveClient(pretendDrive())
        val repo = ContentRepository(context, drive, MediaCache(context, drive))

        assertTrue(repo.sync("folder", downloadMedia = false))
        assertEquals(SyncState.Done(1), repo.sync.value)
        val items = repo.books.value.flatten().filter { it.book.id == "big" }
        assertEquals(chapters * perChapter, items.size)

        // A fresh start reads only the index, not the lessons.
        val again = ContentRepository(context, drive, MediaCache(context, drive))
        timed("loading the index of ${items.size} lessons", 3_000) { again.loadLocal() }
        assertEquals(items.size, again.books.value.flatten().count { it.book.id == "big" })

        // Opening a lesson reads one chapter; its neighbours then come from memory.
        val first = timed("reading a lesson from disk", 1_500) { again.lesson(items[2_750]) }
        assertNotNull(first)
        assertEquals(2, first!!.tracks.size)
        assertEquals("Stem of question 25.0?", first.quiz.single().q)
        timed("reading its neighbour", 50) { again.lesson(items[2_751]) }

        // The scheduler and the tag statistics run on every swipe.
        val now = 1_000L * 86_400_000
        val cards = items.take(3_000).mapIndexed { i, it ->
            it.key to CardState(Phase.REVIEW, interval = 5f, due = now + (i - 1_500) * 3_600_000L, reps = 4, lapses = i % 3)
        }.toMap()
        val keys = items.map { it.key }
        val next = timed("choosing the next lesson", 250) { Scheduler.next(keys, cards, keys.take(3), 0, 0, now, StudySettings()) }
        assertNotNull(next)
        val stats = timed("tag statistics", 500) { Tags.stats(items.map { it.key to it.lesson.tags }, cards, now) }
        assertEquals(items.size, stats.getValue("d:indications").lessons)
        assertTrue(stats.getValue("cardiac").lessons in 1 until items.size)
        timed("filtering by focus", 150) { items.count { Tags.matches(it.lesson.tags, setOf("cardiac/valve", "l:2")) } }
    }
}
