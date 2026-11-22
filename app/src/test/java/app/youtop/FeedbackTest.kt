package app.youtop

import app.youtop.data.Feedback
import app.youtop.data.Note
import app.youtop.data.info
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class FeedbackTest {
    private fun redo(i: Int, text: String = "fix it") =
        Note(Feedback.REDO, "book/ch01/q$i", "Lesson $i", listOf("picture"), text, "story, sentence 2 of 9", i.toLong())

    @Test
    fun aHeldLessonComesBackOnlyWithANewerVersion() {
        fun item(lesson: String, rev: Int) = app.youtop.data.FeedItem(
            app.youtop.data.BookInfo(id = "b", title = "B"), app.youtop.data.ChapterInfo(id = "c", title = "C", source = "chapter:x"),
            app.youtop.data.LessonInfo(id = lesson, title = lesson, rev = rev),
        )
        val held = mapOf("b/c/one" to 0, "b/c/two" to 1, "b/c/gone" to 0)
        // "one" was made again; "two" is the same version as when it was reported; "three" was never held.
        assertEquals(listOf("b/c/one"), Feedback.redone(held, listOf(item("one", 1), item("two", 1), item("three", 5))))
        assertEquals(emptyList<String>(), Feedback.redone(emptyMap(), listOf(item("one", 9))))
    }

    @Test
    fun aLessonWithoutRevIsVersionZero() {
        val lesson = app.youtop.data.ContentJson.decodeFromString(app.youtop.data.Lesson.serializer(), """{"id":"x","title":"X"}""")
        assertEquals(0, lesson.rev)
        assertEquals(3, app.youtop.data.ContentJson.decodeFromString(app.youtop.data.Lesson.serializer(), """{"id":"x","title":"X","rev":3}""").info().rev)
    }

    @Test
    fun nothingWaitingNothingSent() = assertNull(Feedback.issue(emptyList(), "2.6"))

    @Test
    fun aNewerNoteReplacesTheOlderOfTheSameKind() {
        val notes = Feedback.add(Feedback.add(listOf(redo(1, "a")), redo(1, "b")), Note(Feedback.GOOD, "book/ch01/q1"))
        assertEquals(listOf("b", ""), notes.map { it.text })
    }

    @Test
    fun theIssueSaysWhatTheAgentNeeds() {
        val (url, sent) = Feedback.issue(listOf(Note(Feedback.GOOD, "book/ch01/q2", "Two"), redo(1, "line one\nline two")), "2.6")!!
        assertEquals(2, sent.size)
        val text = URLDecoder.decode(url, "UTF-8")
        assertTrue(text, text.contains("title=Lesson feedback: 1 redo, 1 good job"))
        // Redo first, with the parts, where and the owner's words quoted.
        assertTrue(text, text.indexOf("### REDO book/ch01/q1") < text.indexOf("### GOOD book/ch01/q2"))
        assertTrue(text, text.contains("Wrong: picture\nWhere: story, sentence 2 of 9\nNote: \n> line one\n> line two"))
        assertTrue(url, !url.contains("+"))
    }

    @Test
    fun aLongListIsSentInParts() {
        val many = (1..200).map { redo(it, "تتغير الصورة كلها ".repeat(5)) }
        val (url, sent) = Feedback.issue(many, "2.6")!!
        assertTrue(sent.size in 1 until 200)
        assertTrue(url.length <= 7000)
        assertEquals(many.take(sent.size), sent)
    }
}
