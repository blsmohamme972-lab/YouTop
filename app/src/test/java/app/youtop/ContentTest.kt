package app.youtop

import app.youtop.data.Book
import app.youtop.data.ContentJson
import app.youtop.data.DriveClient
import app.youtop.data.flatten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentTest {
    @Test
    fun starterBookIsValid() {
        val book = ContentJson.decodeFromString<Book>(File("src/main/assets/seed/starter.json").readText())
        val lessons = listOf(book).flatten()
        assertEquals(3, lessons.size)
        for (item in lessons) {
            val lesson = item.lesson
            assertEquals(listOf("story", "explanation"), lesson.tracks.map { it.kind })
            assertTrue(lesson.quiz.isNotEmpty())
            lesson.quiz.forEach { assertTrue(it.answer in it.options.indices) }
            lesson.tracks.flatMap { it.segments }.mapNotNull { it.spot }.forEach {
                assertTrue(it.x in 0f..1f && it.y in 0f..1f && it.r > 0f)
            }
            assertTrue(File("src/main/assets/" + lesson.image!!.removePrefix("asset:")).exists())
        }
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
}
