package app.youtop.study

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    /**
     * Smart scheduling: each lesson's memory is modelled (how firmly it is held, how hard it is),
     * and it comes back when the chance of still knowing it has fallen to [retention]. When off,
     * the classic ease-factor rules above are used.
     */
    val smart: Boolean = true,
    /** With [smart]: the share of lessons that should still be known when they come back. */
    val retention: Float = 0.9f,
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
) {
    companion object {
        val Relaxed = StudySettings(newPerDay = 10, dailyGoal = 15)
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
    /** Smart scheduling: days until the chance of recall falls to 90%. 0 = not modelled yet. */
    val stability: Float = 0f,
    /** Smart scheduling: how hard this lesson is for the learner, 1 (easy) to 10. 0 = not modelled yet. */
    val difficulty: Float = 0f,
) {
    /** A lesson that keeps being forgotten however often it comes back. */
    val isLeech: Boolean get() = lapses >= LEECH_LAPSES

    companion object {
        const val LEECH_LAPSES = 5
    }
}

/**
 * The memory model behind smart scheduling: FSRS-4.5 (the model Anki now ships) with its published
 * default weights. Stability is the number of days after which recall has fallen to 90%.
 */
object Memory {
    private val w = floatArrayOf(
        0.4872f, 1.4003f, 3.7145f, 13.8206f, 5.1618f, 1.2298f, 0.8975f, 0.031f, 1.6474f,
        0.1367f, 1.0461f, 2.1072f, 0.0793f, 0.3246f, 1.587f, 0.2272f, 2.8755f,
    )
    private const val DECAY = -0.5f
    private const val FACTOR = 19f / 81f
    private const val MIN_STABILITY = 0.01f

    /** Chance (0..1) of still knowing a lesson [days] after it was last seen. */
    fun recall(days: Float, stability: Float): Float =
        (1f + FACTOR * max(0f, days) / max(MIN_STABILITY, stability)).pow(DECAY)

    /** Days to wait until the chance of recall has fallen to [retention]. */
    fun interval(retention: Float, stability: Float): Float =
        stability / FACTOR * (retention.coerceIn(0.7f, 0.97f).pow(1f / DECAY) - 1f)

    fun firstStability(rating: Rating): Float = w[rating.ordinal]
    fun firstDifficulty(rating: Rating): Float = (w[4] - (rating.ordinal - 2) * w[5]).coerceIn(1f, 10f)

    fun nextDifficulty(difficulty: Float, rating: Rating): Float =
        (w[7] * w[4] + (1f - w[7]) * (difficulty - w[6] * (rating.ordinal - 2))).coerceIn(1f, 10f)

    /** Stability after a lesson was remembered when its chance of recall was [recall]. */
    fun afterSuccess(difficulty: Float, stability: Float, recall: Float, rating: Rating): Float {
        val hard = if (rating == Rating.HARD) w[15] else 1f
        val easy = if (rating == Rating.EASY) w[16] else 1f
        return stability * (1f + exp(w[8]) * (11f - difficulty) * stability.pow(-w[9]) * (exp(w[10] * (1f - recall)) - 1f) * hard * easy)
    }

    /** Stability after a lesson was forgotten: never more than it was. */
    fun afterLapse(difficulty: Float, stability: Float, recall: Float): Float =
        max(MIN_STABILITY, min(stability, w[11] * difficulty.pow(-w[12]) * ((stability + 1f).pow(w[13]) - 1f) * exp(w[14] * (1f - recall))))

    /** A lesson studied before smart scheduling existed: read its memory off its interval and ease. */
    fun difficultyFromEase(ease: Float): Float = (10f - (ease - 1.3f) / 1.2f * 5f).coerceIn(1f, 10f)
}

/** Spaced repetition in the style of Anki's SM-2 scheduler. Pure functions, no Android. */
object Scheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 86_400_000L
    private const val MIN_EASE = 1.3f

    fun review(card: CardState, rating: Rating, now: Long, s: StudySettings, fuzz: Float = 1f): CardState {
        val base = (if (s.smart) remember(card, rating, now) else card).copy(reps = card.reps + 1, seen = now)
        return when (card.phase) {
            Phase.NEW, Phase.LEARNING -> learn(
                base.copy(ease = if (card.phase == Phase.NEW) s.startingEase else card.ease),
                rating, now, s, s.learningSteps, Phase.LEARNING,
            )
            Phase.RELEARNING -> learn(base, rating, now, s, s.relearningSteps, Phase.RELEARNING)
            Phase.REVIEW -> reviewDue(base, rating, now, s, fuzz)
        }
    }

    /**
     * Smart scheduling: updates what is believed about the learner's memory of this lesson.
     * Runs on every rating, in every phase, so the model always reflects the whole history.
     */
    private fun remember(card: CardState, rating: Rating, now: Long): CardState {
        // First rating ever, or a lesson studied before smart scheduling: start from what is known.
        val stability = when {
            card.stability > 0f -> card.stability
            card.interval > 0f -> card.interval
            else -> return card.copy(stability = Memory.firstStability(rating), difficulty = Memory.firstDifficulty(rating))
        }
        val difficulty = if (card.difficulty > 0f) card.difficulty else Memory.difficultyFromEase(card.ease)
        val days = when {
            card.seen > 0L -> (now - card.seen).toFloat() / DAY
            card.phase == Phase.REVIEW -> card.interval + (now - card.due).toFloat() / DAY
            else -> 0f
        }
        val recall = Memory.recall(days, stability)
        return card.copy(
            stability = if (rating == Rating.AGAIN) Memory.afterLapse(difficulty, stability, recall) else Memory.afterSuccess(difficulty, stability, recall, rating),
            difficulty = Memory.nextDifficulty(difficulty, rating),
        )
    }

    private fun learn(card: CardState, rating: Rating, now: Long, s: StudySettings, steps: List<Int>, phase: Phase): CardState {
        fun graduate(days: Float): CardState {
            val interval = when {
                // Smart: wait until the chance of recall has fallen to the target.
                // (A first answer that was merely quick is not trusted with weeks: see easyDays.)
                s.smart && card.stability > 0f -> Memory.interval(s.retention, card.stability)
                    .let { if (card.reps <= 1 && rating == Rating.EASY) min(it, s.easyDays) else it }
                // A relearned lesson keeps the interval it was given when it lapsed.
                phase == Phase.RELEARNING -> max(1f, card.interval)
                else -> days
            }
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
            val kept = if (s.smart) Memory.interval(s.retention, card.stability) else card.interval * s.lapseFactor
            val interval = cap(max(1f, kept), s)
            val lapsed = card.copy(ease = max(MIN_EASE, card.ease - 0.2f), lapses = card.lapses + 1, interval = interval)
            return if (s.relearningSteps.isEmpty()) {
                lapsed.copy(due = now + days(interval))
            } else {
                lapsed.copy(phase = Phase.RELEARNING, step = 0, due = now + s.relearningSteps[0] * MINUTE)
            }
        }
        if (s.smart) {
            // The memory model has already taken in how long was really waited and how it went.
            val interval = cap(Memory.interval(s.retention, card.stability) * fuzz, s)
            return card.copy(interval = interval, due = now + days(interval))
        }
        val (ease, grown) = when (rating) {
            Rating.HARD -> max(MIN_EASE, card.ease - 0.15f) to min(waited, card.interval) * s.hardFactor
            Rating.EASY -> (card.ease + 0.15f) to (card.interval + late) * card.ease * s.easyBonus
            else -> card.ease to (card.interval + late / 2f) * card.ease
        }
        val full = max(card.interval + 1f, grown * s.intervalModifier * fuzz)
        // Reviewed early: credit the growth in proportion to the time that really passed, and never
        // shrink the interval. (A hard answer given early must not earn more than one given on time.)
        val early = waited < card.interval
        val target = if (early) card.interval + (full - card.interval) * (waited / card.interval) else full
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
     * @param dayEnd    when the study day ends ([dayEnd]). A learned lesson is due on its day, not at
     *                  its minute: one studied late last night is ready this afternoon, as in Anki.
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
        dayEnd: Long = now,
    ): String? {
        if (keys.isEmpty()) return null
        val blocked = recent.takeLast(min(3, keys.size - 1)).toSet()
        val open = keys.filter { it !in blocked }
        fun phase(k: String) = cards[k]?.phase ?: Phase.NEW
        fun due(k: String) = cards[k]?.due ?: 0L

        val learning = open.filter { phase(it) == Phase.LEARNING || phase(it) == Phase.RELEARNING }
        learning.filter { due(it) <= now }.minByOrNull(::due)?.let { return it }

        // Of the reviews that are due, the most overdue for its interval first: it is the one most
        // at risk of being forgotten (two days late on a three-day interval beats two days late on sixty).
        val reviews = open.filter { phase(it) == Phase.REVIEW && due(it) <= dayEnd }
            .sortedByDescending { k -> (now - due(k)).toFloat() / max(DAY.toFloat(), (cards[k]?.interval ?: 1f) * DAY) }
        val fresh = open.filter { phase(it) == Phase.NEW }
        fun pickNew() = if (s.randomNewOrder) fresh.random(random) else fresh.first()
        val mayStartNew = fresh.isNotEmpty() && newToday < s.newPerDay
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
    fun rate(answered: Int, wrong: Int, questions: Int, avgAnswerMs: Long, finishedNarration: Boolean, dwellMs: Long): Rating? = when {
        answered > 0 && wrong == 0 && answered >= questions -> if (avgAnswerMs in 1..4_000) Rating.EASY else Rating.GOOD
        answered > 0 && wrong == 0 -> Rating.HARD
        answered > 0 -> if (answered > 1 && wrong * 2 < answered) Rating.HARD else Rating.AGAIN
        dwellMs < 4_000 -> null
        finishedNarration -> if (questions == 0) Rating.GOOD else Rating.HARD
        else -> null
    }

    /** True when a started lesson should be shown now: learning steps by the minute, learned lessons by the day. */
    fun isDue(card: CardState, now: Long, dayEnd: Long): Boolean = when (card.phase) {
        Phase.NEW -> false
        Phase.REVIEW -> card.due <= dayEnd
        else -> card.due <= now
    }

    /** The moment the study day that contains [now] ends (04:00 local time). */
    fun dayEnd(now: Long, zoneOffsetMs: Int): Long = (dayOf(now, zoneOffsetMs) + 1) * DAY + 4 * 3_600_000L - zoneOffsetMs

    /** Day number that rolls over at 04:00 local time, like Anki, so late-night study counts for the same day. */
    fun dayOf(now: Long, zoneOffsetMs: Int): Long = Math.floorDiv(now + zoneOffsetMs - 4 * 3_600_000L, DAY)
}
