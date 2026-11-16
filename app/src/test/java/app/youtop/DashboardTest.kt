package app.youtop

import app.youtop.data.Backup
import app.youtop.data.BookInfo
import app.youtop.data.ChapterInfo
import app.youtop.data.LessonInfo
import app.youtop.study.CardState
import app.youtop.study.Dashboard
import app.youtop.study.DayLog
import app.youtop.study.Phase
import app.youtop.study.Progress
import app.youtop.study.StudySettings
import app.youtop.ui.SEARCH_LIMIT
import app.youtop.ui.searchLessons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTest {
    private val day = 86_400_000L
    private val now = 20_000 * day + 12 * 3_600_000L
    private val today = 20_000L

    private fun book(id: String, lessons: Int) = BookInfo(
        id, "Book $id",
        chapters = listOf(ChapterInfo("c", "Chapter", source = "book:x", lessons = (1..lessons).map { LessonInfo("q$it", "Question $it of $id", tags = listOf("cardiac/aortic-valve")) })),
    )

    /** Topics are counted across books: each tag, and every tag above it, by how often its lessons were studied. */
    @Test
    fun tagsCountHowOftenTheirLessonsWereStudiedAcrossBooks() {
        val lessons = listOf(
            "a/c/q1" to listOf("cardiac/aortic-valve", "d:anatomy"),
            "a/c/q2" to listOf("cardiac/cpb", "d:anatomy"),
            "b/c/q1" to listOf("cardiac/aortic-valve", "d:technique"),   // another book, same topic
            "b/c/q2" to listOf("thoracic/pleura", "d:technique"),
        )
        val cards = mapOf(
            "a/c/q1" to CardState(Phase.REVIEW, due = now + day, reps = 2),
            "b/c/q1" to CardState(Phase.REVIEW, due = now - day, reps = 9, lapses = 1),
            "b/c/q2" to CardState(Phase.LEARNING, due = now + day, reps = 1),
        )
        val stats = app.youtop.study.Tags.stats(lessons, cards, now)
        assertEquals(listOf(0, 0, 1, 0, 0, 1), stats.getValue("cardiac/aortic-valve").studied.toList())
        assertEquals(listOf(1, 0, 1, 0, 0, 1), stats.getValue("cardiac").studied.toList())
        assertEquals(listOf(1, 0, 1, 0, 0, 0), stats.getValue("d:anatomy").studied.toList())
        assertEquals(listOf(0, 1, 0, 0, 0, 0), stats.getValue("thoracic").studied.toList())
        assertEquals(3, stats.getValue("cardiac").lessons)
        assertEquals(1, stats.getValue("cardiac").due)
    }

    @Test
    fun countsWhatIsDueLearnedAndComing() {
        val books = listOf(book("a", 10), book("b", 5))
        val cards = mapOf(
            "a/c/q1" to CardState(Phase.REVIEW, due = now - day),          // learned, due
            "a/c/q2" to CardState(Phase.REVIEW, due = now + day),          // learned, due tomorrow
            "a/c/q3" to CardState(Phase.REVIEW, due = now + 3 * day),      // learned, in three days
            "a/c/q4" to CardState(Phase.LEARNING, due = now - 60_000),     // learning, due
            "a/c/q5" to CardState(Phase.NEW),                              // opened but never rated
            "b/c/q1" to CardState(Phase.REVIEW, due = now - day),          // in a book left out of the feed
            "gone/c/q1" to CardState(Phase.REVIEW, due = now - day),       // a lesson that no longer exists
        )
        val progress = Progress(
            cards = cards, day = today, doneToday = 7,
            history = mapOf(today to DayLog(4, 1), today - 1 to DayLog(10, 2), today - 6 to DayLog(3, 0), today - 7 to DayLog(99, 0), today - 40 to DayLog(50, 50)),
        )
        val d = Dashboard.of(books, excluded = setOf("b"), progress, today, now, zoneOffsetMs = 0)
        assertEquals(10, d.total)
        assertEquals(2, d.due)
        assertEquals(1, d.learning)
        assertEquals(3, d.learned)
        assertEquals(6, d.fresh)
        assertEquals(listOf(3, 0, 0, 0, 0, 10, 7), d.week) // today: the day's counter, which is ahead of the history
        assertEquals(listOf(1, 0, 1, 0, 0, 0, 0), d.forecast)
        assertEquals(1f - 3f / 116f, d.accuracy!!, 0.0001f) // the last thirty days only
        assertEquals(listOf(4, 1), d.books.map { it.started })
        assertEquals(listOf(2, 1), d.books.map { it.due })
        // How often each lesson was studied; a started lesson counts as studied at least once.
        assertEquals(listOf(6, 4, 0, 0, 0, 0), d.books[0].reps)
        val counted = progress.copy(cards = cards + mapOf(
            "a/c/q1" to CardState(Phase.REVIEW, due = now + day, reps = 3),
            "a/c/q2" to CardState(Phase.REVIEW, due = now + day, reps = 12),
            "a/c/q4" to CardState(Phase.LEARNING, due = now, reps = 2),
        ))
        assertEquals(listOf(6, 1, 1, 1, 0, 1), Dashboard.of(books, setOf("b"), counted, today, now, 0).books[0].reps)
        // Studying one book: the totals are that book's, even if it was left out of the mixed feed.
        assertEquals(5, Dashboard.of(books, setOf("b"), progress, today, now, 0, only = "b").total)

        assertEquals(emptyList<String>(), d.leeches)
        assertNull(d.recall) // none of these lessons says when it was last seen

        val hard = progress.copy(cards = cards + mapOf(
            "a/c/q2" to CardState(Phase.REVIEW, interval = 4f, due = now + day, lapses = 7, seen = now - 3 * day, stability = 4f),
            "a/c/q3" to CardState(Phase.REVIEW, interval = 9f, due = now + 3 * day, lapses = 5, seen = now - 6 * day),
        ))
        val h = Dashboard.of(books, setOf("b"), hard, today, now, 0)
        assertEquals(listOf("a/c/q2", "a/c/q3"), h.leeches) // most often forgotten first
        assertTrue(h.recall!! > 0.85f && h.recall!! < 1f)

        val empty = Dashboard.of(emptyList(), emptySet(), Progress(), today, now, 0)
        assertEquals(0, empty.total)
        assertNull(empty.accuracy)
    }

    @Test
    fun aLibraryOfThousandsIsSummarisedAndSearchedQuickly() {
        val books = (1..11).map { book("b$it", 500) }
        val cards = books.flatMap { b -> (1..250).map { "${b.id}/c/q$it" to CardState(Phase.REVIEW, due = now + (it % 9) * day) } }.toMap()
        val start = System.nanoTime()
        val d = Dashboard.of(books, emptySet(), Progress(cards = cards, day = today), today, now, 0)
        val found = searchLessons(books, "question 499", mapOf("cardiac/aortic-valve" to "Aortic valve"))
        val byTopic = searchLessons(books, "aortic b3", mapOf("cardiac/aortic-valve" to "Aortic valve"))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertEquals(5_500, d.total)
        assertEquals(2_750, d.learned)
        assertEquals(11, found.size)
        assertEquals(SEARCH_LIMIT, byTopic.size)
        assertTrue(searchLessons(books, "  ", emptyMap()).isEmpty())
        assertTrue("took $ms ms", ms < 1_500)
    }

    @Test
    fun aBackupComesBackAsItWasAndOtherFilesAreRefused() {
        val progress = Progress(
            cards = mapOf("a/c/q1" to CardState(Phase.REVIEW, interval = 12.5f, ease = 2.3f, due = now, reps = 7, lapses = 2, seen = now - day)),
            day = today, doneToday = 3, newToday = 1, streak = 12, lastStudyDay = today, history = mapOf(today to DayLog(3, 1)),
        )
        val backup = Backup(made = now, progress = progress, study = StudySettings.Intensive.copy(focus = setOf("cardiac")), saved = setOf("a/c/q1"))
        val back = Backup.decode(Backup.encode(backup))!!
        assertEquals(progress, back.progress)
        assertEquals(backup.study, back.study)
        assertEquals(backup.saved, back.saved)

        assertNull(Backup.decode("not json"))
        assertNull(Backup.decode("{}"))
        assertNull(Backup.decode("""{"books":[{"id":"x","file":"y"}]}""")) // a catalog, not a backup
        // Progress saved by an older version (no history) still loads into a backup.
        assertEquals(5, Backup.decode("""{"app":"youtop","progress":{"streak":5,"cards":{}}}""")!!.progress.streak)
    }
}
