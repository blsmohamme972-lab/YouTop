package app.youtop

import app.youtop.data.Book
import app.youtop.data.ContentJson
import app.youtop.data.Question
import app.youtop.data.correct
import app.youtop.data.isUsable
import app.youtop.data.multi
import app.youtop.data.DriveClient
import app.youtop.data.Taxonomy
import app.youtop.study.Tags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentTest {
    @Test
    fun starterBookIsValid() {
        val book = ContentJson.decodeFromString<Book>(File("src/main/assets/seed/starter.json").readText())
        val lessons = book.chapters.flatMap { it.lessons }
        assertEquals(3, lessons.size)
        for (lesson in lessons) {
            assertEquals(listOf("story", "explanation"), lesson.tracks.map { it.kind })
            assertTrue(lesson.quiz.isNotEmpty())
            lesson.quiz.forEach { assertTrue(it.isUsable()) }
            lesson.tracks.flatMap { it.segments }.mapNotNull { it.spot }.forEach {
                assertTrue(it.x in 0f..1f && it.y in 0f..1f && it.r > 0f)
            }
            assertTrue(File("src/main/assets/" + lesson.image!!.removePrefix("asset:")).exists())
        }
    }

    /** "Choose all that apply": [Question.answers] lists every right option, and [Question.answer] is the first. */
    @Test
    fun questionsWithSeveralAnswers() {
        val four = listOf("a", "b", "c", "d")
        val one = Question("q", four, answer = 2)
        assertEquals(false, one.multi)
        assertEquals(setOf(2), one.correct)
        val several = Question("q", four, answer = 0, answers = listOf(0, 2, 3))
        assertEquals(true, several.multi)
        assertEquals(setOf(0, 2, 3), several.correct)
        assertTrue(several.isUsable())
        // Content is never trusted: an answer outside the options, or a first answer not among them, is not asked.
        assertEquals(false, Question("q", four, answer = 0, answers = listOf(0, 4)).isUsable())
        assertEquals(false, Question("q", four, answer = 1, answers = listOf(0, 2)).isUsable())
        // Content written before the field still reads: no "answers" means the single "answer".
        val old = ContentJson.decodeFromString<Question>("""{"q":"q","options":["a","b"],"answer":1}""")
        assertEquals(setOf(1), old.correct)
        val book = ContentJson.decodeFromString<Book>(File("src/main/assets/seed/starter.json").readText())
        assertTrue(book.chapters.flatMap { it.lessons }.flatMap { it.quiz }.any { it.multi })
    }

    @Test
    fun folderIdFromLinkOrBareId() {
        val id = "1AbCdEfGhIjKlMnOpQrStUvWxYz012345"
        assertEquals(id, DriveClient.parseFolderId("https://drive.google.com/drive/folders/$id?usp=sharing"))
        assertEquals(id, DriveClient.parseFolderId("https://drive.google.com/drive/u/0/folders/$id"))
        assertEquals(id, DriveClient.parseFolderId("https://drive.google.com/open?id=$id"))
        assertEquals(id, DriveClient.parseFolderId("  $id "))
        assertNull(DriveClient.parseFolderId("not a link"))
    }

    @Test
    fun newestCatalogWins() {
        fun file(name: String) = DriveClient.Entry(name, name, false)
        val entries = listOf(file("catalog.json"), file("catalog-2.json"), file("catalog-10.json"), file("notes.txt"),
            DriveClient.Entry("f", "catalog-99.json", true))
        assertEquals("catalog-10.json", DriveClient.pickCatalog(entries)?.name)
        assertNull(DriveClient.pickCatalog(listOf(file("book.json"))))
    }

    @Test
    fun parsesPublicFolderPage() {
        val html = """
            <div class="flip-entry" id="entry-1aaaBBBcccDDD_-1" tabindex="0" role="link"><div class="flip-entry-info">
            <a href="https://drive.google.com/file/d/1aaaBBBcccDDD_-1/view?usp=drive_web" target="_blank"><div class="flip-entry-visual"></div>
            <div class="flip-entry-title">catalog-3.json</div></a></div></div>
            <div class="flip-entry" id="entry-2folderID" tabindex="0" role="link"><div class="flip-entry-info">
            <a href="https://drive.google.com/drive/folders/2folderID" target="_blank"><div class="flip-entry-visual"></div>
            <div class="flip-entry-title">Biology &amp; Life</div></a></div></div>
        """.trimIndent()
        val entries = DriveClient.parseFolderPage(html)
        assertEquals(
            listOf(DriveClient.Entry("1aaaBBBcccDDD_-1", "catalog-3.json", false), DriveClient.Entry("2folderID", "Biology & Life", true)),
            entries,
        )
    }

    /** A real page saved from Drive, to notice if the parser stops matching its markup. */
    @Test
    fun parsesSavedDrivePage() {
        val entries = DriveClient.parseFolderPage(javaClass.getResource("/drive-folder-page.html")!!.readText())
        assertEquals(
            listOf(
                DriveClient.Entry("1toVwQTGBpZHtZA14kIlO4xR5XRRJIAuU", "YouTop Starter", true),
                DriveClient.Entry("1GRjzQnJfrCzD4zzfrQ_z9-3Dp-8mb9fC", "catalog-1.json", false),
            ),
            entries,
        )
        assertEquals("catalog-1.json", DriveClient.pickCatalog(entries)?.name)
    }

    @Test
    fun tagSystemCoversTheThreeSpecialties() {
        val taxonomy = ContentJson.decodeFromString<Taxonomy>(File("src/main/assets/taxonomy.json").readText())
        assertEquals(listOf("topic", "d", "q", "l", "x"), taxonomy.facets.map { it.id })
        assertEquals(listOf("cardiac", "thoracic", "vascular"), taxonomy.facets[0].nodes.map { it.id })
        assertEquals("Aortic valve", taxonomy.titles["cardiac/aortic-valve"])
        assertEquals("Clinical vignette", taxonomy.titles["q:vignette"])
        assertTrue(taxonomy.titles.size in 40..60) // kept small on purpose
    }

    @Test
    fun focusMatchesWithinAGroupAndAcrossGroups() {
        val tags = listOf("cardiac/aortic-valve", "d:indications", "q:vignette", "l:2")
        assertTrue(Tags.matches(tags, emptySet()))
        assertTrue(Tags.matches(tags, setOf("cardiac")))
        assertTrue(Tags.matches(tags, setOf("cardiac/aortic-valve", "vascular"))) // either topic
        assertTrue(Tags.matches(tags, setOf("cardiac/aortic-valve", "d:indications"))) // topic and knowledge type
        assertTrue(!Tags.matches(tags, setOf("cardiac/aortic-valve", "d:anatomy")))
        assertTrue(!Tags.matches(tags, setOf("cardiac/aortic-valve-x")))
        assertTrue(!Tags.matches(emptyList(), setOf("cardiac")))
        assertEquals(listOf("cardiac", "cardiac/aortic-valve"), Tags.withAncestors("cardiac/aortic-valve"))
        assertEquals(listOf("d:indications"), Tags.withAncestors("d:indications"))
        assertEquals("cardiac/aortic-valve", Tags.mainTopic(tags))
    }
}
