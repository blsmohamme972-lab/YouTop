package app.youtop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Index file in the root of the Drive folder: `catalog-<number>.json`. Highest number wins. */
@Serializable
data class Catalog(val books: List<BookRef> = emptyList())

/** [file] is the Drive file id of the book's JSON. A new upload has a new id, which is how updates are detected. */
@Serializable
data class BookRef(val id: String, val file: String)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val description: String? = null,
    val chapters: List<Chapter> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val lessons: List<Lesson> = emptyList(),
)

/**
 * One fullscreen "short".
 * Media references ([image], [Track.audio]) are `drive:<fileId>`, `asset:<path>` or an https URL.
 */
@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val image: String? = null,
    val tracks: List<Track> = emptyList(),
    val quiz: List<Question> = emptyList(),
)

/** A narration: `kind` is "story" or "explanation", `lang` a language code such as "ar" or "en". */
@Serializable
data class Track(
    val kind: String = "story",
    val lang: String = "ar",
    val audio: String? = null,
    val segments: List<Segment> = emptyList(),
)

/**
 * One sentence of narration. [t] is where it starts in the audio file (milliseconds);
 * [spot] is what the camera points at while it is spoken.
 */
@Serializable
data class Segment(val text: String, val t: Long? = null, val spot: Spot? = null)

/** Region of the image: centre [x],[y] as fractions of image width/height, radius [r] as a fraction of image width. */
@Serializable
data class Spot(val x: Float, val y: Float, val r: Float = 0.15f)

@Serializable
data class Question(
    val q: String,
    val options: List<String>,
    val answer: Int,
    val explain: String = "",
    val lang: String = "en",
)

data class FeedItem(val book: Book, val chapter: Chapter, val lesson: Lesson) {
    val key: String = "${book.id}/${chapter.id}/${lesson.id}"
}

fun List<Book>.flatten(): List<FeedItem> =
    flatMap { b -> b.chapters.flatMap { c -> c.lessons.map { FeedItem(b, c, it) } } }

fun Book.mediaRefs(): List<String> =
    chapters.flatMap { c -> c.lessons.flatMap { l -> listOfNotNull(l.image) + l.tracks.mapNotNull { it.audio } } }

val ContentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
