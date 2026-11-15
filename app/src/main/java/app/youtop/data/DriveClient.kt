package app.youtop.data

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Reads a Google Drive folder that is shared as "anyone with the link".
 * Uses the Drive API when an API key is configured, otherwise the public folder page.
 */
class DriveClient(private val http: OkHttpClient, private val apiKey: String = "") {

    data class Entry(val id: String, val name: String, val isFolder: Boolean)

    fun list(folderId: String): List<Entry> =
        if (apiKey.isNotBlank()) listWithApi(folderId) else listPublicPage(folderId)

    private fun listWithApi(folderId: String): List<Entry> {
        val out = ArrayList<Entry>()
        var pageToken: String? = null
        do {
            val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "'$folderId' in parents and trashed = false")
                .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType)")
                .addQueryParameter("pageSize", "1000")
                .addQueryParameter("key", apiKey)
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()
            val page = ContentJson.decodeFromString<ApiPage>(getText(url.toString()))
            page.files.mapTo(out) { Entry(it.id, it.name, it.mimeType == FOLDER_MIME) }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return out
    }

    private fun listPublicPage(folderId: String): List<Entry> =
        parseFolderPage(getText("https://drive.google.com/embeddedfolderview?id=$folderId#list"))

    /** A JSON file. Drive answers with a web page when a file is not public or is being throttled; that is an error. */
    fun text(fileId: String): String = getText(downloadUrl(fileId)).also {
        if (!it.trimStart().startsWith("{")) throw IOException("Drive did not return the file $fileId")
    }

    fun download(fileId: String, dest: File) {
        // Its own temporary file, so two downloads of one picture can never write into each other.
        val tmp = File.createTempFile(dest.name + ".", ".part", dest.parentFile)
        try {
            http.newCall(Request.Builder().url(downloadUrl(fileId)).build()).execute().use { res ->
                if (!res.isSuccessful) throw IOException("Drive returned ${res.code} for $fileId")
                if (res.header("Content-Type").orEmpty().startsWith("text/html")) {
                    throw IOException("File $fileId is not shared publicly")
                }
                val body = res.body ?: throw IOException("Empty response for $fileId")
                tmp.outputStream().use { body.byteStream().copyTo(it) }
            }
            if (!tmp.renameTo(dest)) throw IOException("Could not store $fileId")
        } finally {
            tmp.delete()
        }
    }

    private fun getText(url: String): String =
        http.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Drive returned ${res.code}")
            res.body?.string() ?: throw IOException("Empty response")
        }

    @Serializable
    private data class ApiPage(val files: List<ApiFile> = emptyList(), val nextPageToken: String? = null)

    @Serializable
    private data class ApiFile(val id: String, val name: String, val mimeType: String = "")

    companion object {
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"

        fun downloadUrl(fileId: String) =
            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"

        private val ENTRY = Regex(
            """id="entry-([\w-]+)"[\s\S]*?<a href="([^"]*)"[\s\S]*?class="flip-entry-title">([^<]*)<"""
        )

        fun parseFolderPage(html: String): List<Entry> =
            ENTRY.findAll(html).map { m ->
                Entry(
                    id = m.groupValues[1],
                    name = unescape(m.groupValues[3]).trim(),
                    isFolder = m.groupValues[2].contains("/folders/"),
                )
            }.toList()

        private fun unescape(s: String) = s
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'")

        private val ID_IN_LINK = Regex("""(?:/folders/|[?&]id=)([\w-]{10,})""")
        private val BARE_ID = Regex("""[\w-]{20,}""")

        /** Accepts a Drive folder link or a bare folder id. */
        fun parseFolderId(input: String): String? {
            val s = input.trim()
            ID_IN_LINK.find(s)?.let { return it.groupValues[1] }
            return s.takeIf { BARE_ID.matches(it) }
        }

        private val CATALOG_NAME = Regex("""catalog(?:-(\d+))?\.json""", RegexOption.IGNORE_CASE)

        /** Picks `catalog-<n>.json` with the highest n (plain `catalog.json` counts as 0). */
        fun pickCatalog(entries: List<Entry>): Entry? =
            entries.filter { !it.isFolder }
                .mapNotNull { e -> CATALOG_NAME.matchEntire(e.name)?.let { e to (it.groupValues[1].toLongOrNull() ?: 0L) } }
                .maxByOrNull { it.second }?.first
    }
}
