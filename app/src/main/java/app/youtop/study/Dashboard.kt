package app.youtop.study

import app.youtop.data.BookInfo

/** Where one book stands. */
data class BookStat(val id: String, val title: String, val total: Int, val started: Int, val learned: Int, val due: Int)

/** Everything the dashboard shows, worked out in one pass over the library. */
data class Dashboard(
    val total: Int = 0,
    /** Ready to be reviewed now. */
    val due: Int = 0,
    /** Started, still in its first learning steps (or being relearned). */
    val learning: Int = 0,
    /** Learned: now only comes back at growing intervals. */
    val learned: Int = 0,
    /** Never opened. */
    val fresh: Int = 0,
    /** Lessons done on each of the last seven days, oldest first, today last. */
    val week: List<Int> = List(7) { 0 },
    /** Share answered right over the last thirty days; null until there is enough to go on. */
    val accuracy: Float? = null,
    /** Reviews coming due on each of the next seven days, tomorrow first. */
    val forecast: List<Int> = List(7) { 0 },
    val books: List<BookStat> = emptyList(),
    /** Lessons that keep being forgotten ([CardState.isLeech]), hardest first: their keys. */
    val leeches: List<String> = emptyList(),
    /** Average chance, right now, of still knowing a learned lesson; null when nothing is learned yet. */
    val recall: Float? = null,
) {
    companion object {
        /**
         * [excluded] books are listed but do not count towards the totals (they are not in the feed).
         * [today] is the day number from [Scheduler.dayOf]; [zoneOffsetMs] turns a due time into its day.
         */
        fun of(books: List<BookInfo>, excluded: Set<String>, progress: Progress, today: Long, now: Long, zoneOffsetMs: Int): Dashboard {
            var total = 0; var due = 0; var learning = 0; var learned = 0
            val forecast = IntArray(7)
            val dayEnd = Scheduler.dayEnd(now, zoneOffsetMs)
            val leeches = ArrayList<Pair<String, Int>>()
            var recallSum = 0.0; var recallCount = 0
            val stats = books.map { book ->
                var bTotal = 0; var bStarted = 0; var bLearned = 0; var bDue = 0
                val counts = book.id !in excluded
                val prefix = book.id + "/"
                for (chapter in book.chapters) {
                    val chapterPrefix = prefix + chapter.id + "/"
                    for (lesson in chapter.lessons) {
                        bTotal++
                        val key = chapterPrefix + lesson.id
                        val card = progress.cards[key] ?: continue
                        if (card.phase == Phase.NEW) continue
                        bStarted++
                        val isDue = Scheduler.isDue(card, now, dayEnd)
                        if (isDue) bDue++
                        if (card.phase == Phase.REVIEW) bLearned++
                        if (!counts) continue
                        if (card.phase == Phase.REVIEW) learned++ else learning++
                        if (card.isLeech) leeches += key to card.lapses
                        if (card.phase == Phase.REVIEW && card.seen > 0L) {
                            val held = if (card.stability > 0f) card.stability else card.interval
                            recallSum += Memory.recall((now - card.seen).toFloat() / 86_400_000L, held); recallCount++
                        }
                        if (isDue) due++ else {
                            val inDays = Scheduler.dayOf(card.due, zoneOffsetMs) - today
                            if (inDays in 1..7) forecast[(inDays - 1).toInt()]++
                        }
                    }
                }
                if (counts) total += bTotal
                BookStat(book.id, book.title, bTotal, bStarted, bLearned, bDue)
            }
            val week = (6 downTo 0).map { back ->
                val logged = progress.history[today - back]?.done ?: 0
                // The day's counter also covers lessons done before the history existed.
                if (back == 0 && progress.day == today) maxOf(logged, progress.doneToday) else logged
            }
            var done = 0; var again = 0
            for ((day, log) in progress.history) if (day > today - 30) { done += log.done; again += log.again }
            return Dashboard(
                total = total, due = due, learning = learning, learned = learned, fresh = total - learning - learned,
                week = week, accuracy = if (done >= 5) 1f - again.toFloat() / done else null,
                forecast = forecast.toList(), books = stats,
                leeches = leeches.sortedByDescending { it.second }.map { it.first },
                recall = if (recallCount > 0) (recallSum / recallCount).toFloat() else null,
            )
        }
    }
}
