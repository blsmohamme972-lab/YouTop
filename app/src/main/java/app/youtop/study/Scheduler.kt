package app.youtop.study

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Everything the learner can tune. Times are minutes for steps and days for intervals. */
@Serializable
data class StudySettings(
    // Daily load
    val newPerDay: Int = 20,
    val dailyGoal: Int = 30,
    val reviewsPerNew: Int = 3,
    val randomNewOrder: Boolean = false,
    /** What the feed shows once nothing is due: "more" new lessons, review "ahead", or "stop". */
    val afterGoal: String = "more",
    // Learning
    val learningSteps: List<Int> = listOf(1, 10),
    val relearningSteps: List<Int> = listOf(10),
    val graduatingDays: Float = 1f,
    val easyDays: Float = 4f,
    // Reviews
    val startingEase: Float = 2.5f,
    val easyBonus: Float = 1.3f,
    val hardFactor: Float = 1.2f,
    val intervalModifier: Float = 1f,
    /** Share of the old interval kept after a wrong answer (0 = start again from one day). */
    val lapseFactor: Float = 0f,
    val maxDays: Float = 365f,
    // Feed behaviour
    /** Open each lesson straight on its question; the video plays only if asked for. */
    val questionFirst: Boolean = false,
    val autoContinue: Boolean = true,
    val autoAdvance: Boolean = true,
    val shuffleOptions: Boolean = true,
    val speakQuiz: Boolean = false,
    val excludedBooks: Set<String> = emptySet(),
    /** Tags the feed is limited to (see [Tags.matches]); empty means everything. */
    val focus: Set<String> = emptySet(),
    /** Start new lessons from the topics that are going worst. */
    val weakFirst: Boolean = true,
    // The smart scheduler
    /** "smart": the memory model in [Memory] decides the intervals. "classic": the fixed multipliers below. */
    val engine: String = "smart",
    /** Chance of still remembering a lesson when it comes back (smart scheduler). Higher = more reviews. */
    val retention: Float = 0.9f,
    /** Day number ([Scheduler.dayOf]) of the exam, or 0. Until then no lesson is sent further away than the exam. */
    val examDay: Long = 0,
    /** Stretch or shrink intervals by how the learner's own reviews go ([Memory.personalFactor]). */
    val adaptive: Boolean = true,
    /** No new lessons while more reviews are waiting than the daily goal. */
    val holdNew: Boolean = true,
) {
    val smart: Boolean get() = engine != "classic"

    companion object {
        val Relaxed = StudySettings(newPerDay = 10, dailyGoal = 15, retention = 0.85f)
        val Standard = StudySettings()
        /** Short intervals and a heavy daily load, for the weeks before an exam. */
        val Intensive = StudySettings(
            newPerDay = 60, dailyGoal = 100, reviewsPerNew = 2, learningSteps = listOf(1, 5, 20),
            relearningSteps = listOf(5, 20), easyDays = 3f, intervalModifier = 0.7f, maxDays = 30f, retention = 0.94f,
        )
    }
}

enum class Phase { NEW, LEARNING, REVIEW, RELEARNING }

enum class Rating { AGAIN, HARD, GOOD, EASY }

/** Memory state of one lesson. [interval] is in days, [due] and [seen] are epoch milliseconds. */
@Serializable
data class CardState(
    val phase: Phase = Phase.NEW,
    val step: Int = 0,
    val interval: Float = 0f,
    val ease: Float = 2.5f,
    val due: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
    val seen: Long = 0L,
    /** Smart scheduler: days until the chance of remembering falls to 90%. 0 = not worked out yet. */
    val stability: Float = 0f,
    /** Smart scheduler: 1 (easy) to 10 (hard). */
    val difficulty: Float = 0f,
)

/**
 * Spaced repetition. Two ways of deciding the intervals share the same phases and learning steps:
 * the smart one (the memory model in [Memory], aiming at a chosen chance of remembering) and the
 * classic one (Anki's SM-2 multipliers). Pure functions, no Android.
 */
object Scheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 86_400_000L
    private const val MIN_EASE = 1.3f

    /**
     * The lesson's state after an answer.
     * @param fuzz      0.95..1.05, different for each lesson, so lessons learned together do not all return on one day
     * @param personal  the learner's own factor ([Memory.personalFactor]); 1 = as the model expects
     * @param today     day number of [now], for the exam date
     */
    fun review(
        card: CardState, rating: Rating, now: Long, s: StudySettings,
        fuzz: Float = 1f, personal: Float = 1f, today: Long = dayOf(now, 0),
    ): CardState {
        val base = card.copy(reps = card.reps + 1, seen = now)
        if (s.smart) return reviewSmart(base, card, rating, now, s, fuzz, personal, today)
        return when (card.phase) {
            Phase.NEW, Phase.LEARNING -> learn(
                base.copy(ease = if (card.phase == Phase.NEW) s.startingEase else card.ease),
                rating, now, s, s.learningSteps, Phase.LEARNING,
            )
            Phase.RELEARNING -> learn(base, rating, now, s, s.relearningSteps, Phase.RELEARNING)
            Phase.REVIEW -> reviewDue(base, rating, now, s, fuzz)
        }
    }

    // ---- smart ---------------------------------------------------------------

    /** What the model knows about [card], or null for a lesson never answered. */
    fun memory(card: CardState): Memory.State? = when {
        card.stability > 0f -> Memory.State(card.stability, card.difficulty.coerceIn(1f, 10f))
        card.phase == Phase.NEW -> null
        else -> Memory.fromClassic(card.interval, card.ease) // learned before the smart scheduler
    }

    /** Chance (0..1) that [card] is remembered at [now]; null for a lesson never answered. */
    fun retrievability(card: CardState, now: Long): Float? {
        val m = memory(card) ?: return null
        return Memory.retrievability((now - card.seen).toFloat() / DAY, m.s)
    }

    /**
     * Days until a lesson of this [stability] should return: when the chance of remembering it
     * has fallen to the wanted retention, never further away than the exam.
     */
    fun smartInterval(stability: Float, s: StudySettings, fuzz: Float, personal: Float, today: Long): Float {
        var days = Memory.interval(stability * personal, s.retention) * fuzz
        val toExam = s.examDay - today
        if (toExam >= 1) {
            // Back once more before the exam, spread over the last stretch rather than all on its eve.
            val spread = ((fuzz - 0.95f) / 0.10f).coerceIn(0f, 1f)
            days = min(days, max(1f, toExam * (0.55f + 0.35f * spread)))
        }
        return cap(days, s)
    }

    private fun reviewSmart(
        base: CardState, before: CardState, rating: Rating, now: Long, s: StudySettings,
        fuzz: Float, personal: Float, today: Long,
    ): CardState {
        val known = memory(before)
        val waited = if (before.seen > 0L) max(0f, (now - before.seen).toFloat() / DAY) else 0f
        val m = if (known == null) Memory.first(rating) else Memory.next(known, rating, waited)
        val card = base.copy(stability = m.s, difficulty = m.d)
        fun interval() = smartInterval(m.s, s, fuzz, personal, today)
        fun inReview(lapsed: Boolean = false): CardState {
            val days = interval()
            return card.copy(phase = Phase.REVIEW, step = 0, interval = days, due = now + days(days), lapses = card.lapses + if (lapsed) 1 else 0)
        }
        fun stepped(phase: Phase, steps: List<Int>): CardState {
            fun at(step: Int) = card.copy(phase = phase, step = step, due = now + steps[step] * MINUTE)
            if (steps.isEmpty()) return inReview()
            val step = card.step.coerceIn(0, steps.lastIndex)
            return when (rating) {
                Rating.AGAIN -> at(0)
                Rating.HARD -> at(step)
                Rating.GOOD -> if (step + 1 >= steps.size) inReview() else at(step + 1)
                Rating.EASY -> inReview()
            }
        }
        return when (before.phase) {
            Phase.NEW, Phase.LEARNING -> stepped(Phase.LEARNING, s.learningSteps)
            Phase.RELEARNING -> stepped(Phase.RELEARNING, s.relearningSteps)
            Phase.REVIEW ->
                if (rating != Rating.AGAIN) {
                    val days = interval()
                    card.copy(interval = days, due = now + days(days))
                } else if (s.relearningSteps.isEmpty()) {
                    inReview(lapsed = true)
                } else {
                    card.copy(phase = Phase.RELEARNING, step = 0, lapses = card.lapses + 1, interval = interval(), due = now + s.relearningSteps[0] * MINUTE)
                }
        }
    }

    /**
     * What the settings mean in practice: the gaps (days) before each of the next reviews of a
     * new lesson that is answered right every time. Shown beside the settings.
     */
    fun preview(s: StudySettings, personal: Float = 1f, today: Long = 0, reviews: Int = 5): List<Float> {
        var card = CardState()
        var now = 0L
        val out = ArrayList<Float>()
        var guard = 0
        while (out.size < reviews && guard++ < 40) {
            card = review(card, Rating.GOOD, now, s, 1f, personal, today)
            if (card.phase == Phase.REVIEW) out += card.interval
            now = card.due
        }
        return out
    }

    // ---- classic -------------------------------------------------------------

    private fun learn(card: CardState, rating: Rating, now: Long, s: StudySettings, steps: List<Int>, phase: Phase): CardState {
        fun graduate(days: Float): CardState {
            // A relearned lesson keeps the interval it was given when it lapsed.
            val interval = if (phase == Phase.RELEARNING) max(1f, card.interval) else days
            return card.copy(phase = Phase.REVIEW, step = 0, interval = cap(interval, s), due = now + days(cap(interval, s)))
        }
        fun at(step: Int) = card.copy(phase = phase, step = step, due = now + steps[step] * MINUTE)
        if (steps.isEmpty()) return graduate(if (rating == Rating.EASY) s.easyDays else s.graduatingDays)
        val step = card.step.coerceIn(0, steps.lastIndex)
        return when (rating) {
            Rating.AGAIN -> at(0)
            Rating.HARD -> at(step)
            Rating.GOOD -> if (step + 1 >= steps.size) graduate(s.graduatingDays) else at(step + 1)
            Rating.EASY -> graduate(s.easyDays)
        }
    }

    private fun reviewDue(card: CardState, rating: Rating, now: Long, s: StudySettings, fuzz: Float): CardState {
        // Days actually waited: more than planned when overdue, fewer when reviewed ahead of time.
        val waited = max(0f, card.interval + (now - card.due).toFloat() / DAY)
        val late = max(0f, waited - card.interval)
        if (rating == Rating.AGAIN) {
            val interval = cap(max(1f, card.interval * s.lapseFactor), s)
            val lapsed = card.copy(ease = max(MIN_EASE, card.ease - 0.2f), lapses = card.lapses + 1, interval = interval)
            return if (s.relearningSteps.isEmpty()) {
                lapsed.copy(due = now + days(interval))
            } else {
                lapsed.copy(phase = Phase.RELEARNING, step = 0, due = now + s.relearningSteps[0] * MINUTE)
            }
        }
        val (ease, grown) = when (rating) {
            Rating.HARD -> max(MIN_EASE, card.ease - 0.15f) to min(waited, card.interval) * s.hardFactor
            Rating.EASY -> (card.ease + 0.15f) to (card.interval + late) * card.ease * s.easyBonus
            else -> card.ease to (card.interval + late / 2f) * card.ease
        }
        // Reviewed early: credit only the time that really passed, and never shrink the interval.
        val early = waited < card.interval
        val target = if (early) max(card.interval, waited * ease) else max(card.interval + 1f, grown * s.intervalModifier * fuzz)
        val interval = cap(target, s)
        return card.copy(ease = ease, interval = interval, due = now + days(interval))
    }

    private fun cap(days: Float, s: StudySettings) = days.coerceIn(1f, max(1f, s.maxDays))
    private fun days(d: Float) = (d * DAY).toLong()

    /**
     * Chooses what the feed shows next, or null when the day is finished and the learner chose to stop.
     *
     * @param keys      every lesson that may appear, in book order
     * @param recent    lessons shown most recently (newest last) or already queued; not repeated straight away
     * @param newToday  new lessons already started today
     * @param sinceNew  reviews shown since the last new lesson
     */
    fun next(
        keys: List<String>,
        cards: Map<String, CardState>,
        recent: List<String>,
        newToday: Int,
        sinceNew: Int,
        now: Long,
        s: StudySettings,
        random: Random = Random.Default,
    ): String? {
        if (keys.isEmpty()) return null
        val blocked = recent.takeLast(min(3, keys.size - 1)).toSet()
        val open = keys.filter { it !in blocked }
        fun phase(k: String) = cards[k]?.phase ?: Phase.NEW
        fun due(k: String) = cards[k]?.due ?: 0L

        val learning = open.filter { phase(it) == Phase.LEARNING || phase(it) == Phase.RELEARNING }
        learning.filter { due(it) <= now }.minByOrNull(::due)?.let { return it }

        val reviews = open.filter { phase(it) == Phase.REVIEW && due(it) <= now }.sortedBy(::due)
        val fresh = open.filter { phase(it) == Phase.NEW }
        fun pickNew() = if (s.randomNewOrder) fresh.random(random) else fresh.first()
        // A pile of waiting reviews comes first: new lessons would only make it grow.
        val piled = s.holdNew && reviews.size > max(s.dailyGoal, 1)
        val mayStartNew = fresh.isNotEmpty() && newToday < s.newPerDay && !piled
        if (mayStartNew && (reviews.isEmpty() || sinceNew >= s.reviewsPerNew)) return pickNew()
        reviews.firstOrNull()?.let { return it }

        // Nothing is due. Lessons still being learned come back a little early rather than leaving a gap.
        learning.minByOrNull(::due)?.let { return it }
        if (s.afterGoal == "stop") return null
        return when (s.afterGoal) {
            "ahead" -> open.filter { phase(it) == Phase.REVIEW }.minByOrNull(::due) ?: fresh.firstOrNull()?.let { pickNew() }
            else -> fresh.firstOrNull()?.let { pickNew() } ?: open.filter { phase(it) == Phase.REVIEW }.minByOrNull(::due)
        } ?: open.firstOrNull()
    }

    /**
     * Turns what the learner did with a lesson into a rating, so no "how well did you know this?"
     * buttons are needed. Returns null when the lesson was skipped and should simply come back later.
     */
    fun rate(answered: Int, wrong: Int, questions: Int, fast: Boolean, finishedNarration: Boolean, dwellMs: Long): Rating? = when {
        answered > 0 && wrong == 0 && answered >= questions -> if (fast) Rating.EASY else Rating.GOOD
        answered > 0 && wrong == 0 -> Rating.HARD
        answered > 0 -> if (answered > 1 && wrong * 2 < answered) Rating.HARD else Rating.AGAIN
        dwellMs < 4_000 -> null
        finishedNarration -> if (questions == 0) Rating.GOOD else Rating.HARD
        else -> null
    }

    /**
     * An answer counts as fast (the lesson was easy) when it came sooner than a quick reader
     * needs for the question and its options: two seconds plus 35 ms a character.
     */
    fun isFast(tookMs: Long, characters: Int): Boolean = tookMs in 1..(2_000L + 35L * characters)

    /** Day number that rolls over at 04:00 local time, like Anki, so late-night study counts for the same day. */
    fun dayOf(now: Long, zoneOffsetMs: Int): Long = Math.floorDiv(now + zoneOffsetMs - 4 * 3_600_000L, DAY)
}
