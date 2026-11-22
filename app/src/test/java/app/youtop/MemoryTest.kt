package app.youtop

import app.youtop.study.Calibration
import app.youtop.study.CardState
import app.youtop.study.Memory
import app.youtop.study.Phase
import app.youtop.study.Rating
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/** The smart scheduler: the memory model, the intervals it leads to, and how it fits itself to the learner. */
class MemoryTest {
    private val s = StudySettings()
    private val minute = 60_000L
    private val day = 86_400_000L
    private val t0 = 1_000 * day

    @Test
    fun theModelsCurveMeansWhatItSays() {
        assertEquals(1f, Memory.retrievability(0f, 10f), 0.0001f)
        assertEquals(0.9f, Memory.retrievability(10f, 10f), 0.0001f)   // stability = days to 90%
        assertTrue(Memory.retrievability(30f, 10f) < Memory.retrievability(20f, 10f))
        assertEquals(10f, Memory.interval(10f, 0.9f), 0.001f)
        assertTrue(Memory.interval(10f, 0.95f) < 10f)                   // surer of remembering = sooner
        assertTrue(Memory.interval(10f, 0.80f) > 20f)
        // The interval is exactly when the chance has fallen to the wanted one.
        for (r in listOf(0.8f, 0.85f, 0.93f, 0.97f)) assertEquals(r, Memory.retrievability(Memory.interval(7f, r), 7f), 0.0005f)
    }

    @Test
    fun aNewLessonIsLearnedInStepsAndThenSpacedByItsOwnStability() {
        var c = Scheduler.review(CardState(), Rating.GOOD, t0, s)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + 10 * minute, c.due)
        assertEquals(3.173f, c.stability, 0.001f)
        c = Scheduler.review(c, Rating.GOOD, c.due, s)
        assertEquals(Phase.REVIEW, c.phase)
        assertTrue("first gap ${c.interval}", c.interval in 3.5f..5.5f)
        assertEquals(c.stability, c.interval, 0.01f) // at 90% the gap is the stability

        // Answered right when due, the gaps grow fast and then stop at the longest one allowed.
        val gaps = ArrayList<Float>()
        repeat(6) { c = Scheduler.review(c, Rating.GOOD, c.due, s); gaps += c.interval }
        assertTrue("$gaps", gaps.take(4).zipWithNext().all { (a, b) -> b > a * 2f })
        assertTrue("$gaps", gaps[0] in 10f..20f && gaps[1] in 30f..60f)
        assertEquals(365f, gaps.last(), 0.001f)
        assertEquals(gaps.take(4), Scheduler.preview(s).drop(1))

        // An answer that came at once skips the steps and waits about two weeks.
        val easy = Scheduler.review(CardState(), Rating.EASY, t0, s)
        assertEquals(Phase.REVIEW, easy.phase)
        assertTrue(easy.interval in 14f..17f)
        // A wrong first answer starts from far less.
        val again = Scheduler.review(CardState(), Rating.AGAIN, t0, s)
        assertEquals(t0 + minute, again.due)
        assertTrue(again.stability < 0.5f && again.difficulty > easy.difficulty)
    }

    @Test
    fun hardGoodAndEasyAreOrderedAndForgettingCollapsesTheGap() {
        val card = CardState(Phase.REVIEW, interval = 20f, due = t0, seen = t0 - 20 * day, stability = 20f, difficulty = 5f, reps = 4)
        val hard = Scheduler.review(card, Rating.HARD, t0, s)
        val good = Scheduler.review(card, Rating.GOOD, t0, s)
        val easy = Scheduler.review(card, Rating.EASY, t0, s)
        assertTrue(20f < hard.interval && hard.interval < good.interval && good.interval < easy.interval)
        assertTrue(hard.difficulty > good.difficulty && good.difficulty > easy.difficulty)

        val lapsed = Scheduler.review(card, Rating.AGAIN, t0, s)
        assertEquals(Phase.RELEARNING, lapsed.phase)
        assertEquals(1, lapsed.lapses)
        assertEquals(t0 + 10 * minute, lapsed.due)
        assertTrue(lapsed.stability < 5f && lapsed.difficulty > card.difficulty)
        val back = Scheduler.review(lapsed, Rating.GOOD, lapsed.due, s)
        assertEquals(Phase.REVIEW, back.phase)
        assertTrue("back after ${back.interval}", back.interval in 1f..6f)

        // The later a lesson is still remembered, the more that says about it.
        val late = Scheduler.review(card, Rating.GOOD, t0 + 20 * day, s)
        assertTrue(late.interval > good.interval)
        // Reviewed ahead of time, a right answer says little: the gap hardly grows.
        val early = Scheduler.review(card.copy(due = t0 + 19 * day, seen = t0 - day), Rating.GOOD, t0, s)
        assertTrue(early.interval < good.interval && early.interval >= 20f * 0.95f)
        // A harder lesson grows more slowly than an easier one.
        assertTrue(Scheduler.review(card.copy(difficulty = 9f), Rating.GOOD, t0, s).interval < Scheduler.review(card.copy(difficulty = 2f), Rating.GOOD, t0, s).interval)
    }

    @Test
    fun theWantedRetentionTheLongestGapAndTheExamAllBoundTheInterval() {
        val card = CardState(Phase.REVIEW, interval = 30f, due = t0, seen = t0 - 30 * day, stability = 30f, difficulty = 5f)
        fun gap(settings: StudySettings, fuzz: Float = 1f, today: Long = 1_000) = Scheduler.review(card, Rating.GOOD, t0, settings, fuzz, 1f, today).interval
        assertTrue(gap(s.copy(retention = 0.95f)) < gap(s) && gap(s) < gap(s.copy(retention = 0.85f)))
        assertEquals(40f, gap(s.copy(maxDays = 40f)), 0.001f)

        // An exam in 20 days: back before it, and not every lesson on the same day.
        val exam = s.copy(examDay = 1_020)
        val first = gap(exam, fuzz = 0.95f)
        val last = gap(exam, fuzz = 1.05f)
        assertTrue("$first $last", first in 10f..12f && last in 17f..19f)
        assertEquals(1f, gap(s.copy(examDay = 1_001)), 0.001f)
        // After the exam, and for a short gap anyway, the date changes nothing.
        assertEquals(gap(s), gap(s.copy(examDay = 990)), 0.001f)
        val young = CardState(Phase.REVIEW, interval = 2f, due = t0, seen = t0 - 2 * day, stability = 2f, difficulty = 5f)
        assertEquals(Scheduler.review(young, Rating.GOOD, t0, s).interval, Scheduler.review(young, Rating.GOOD, t0, exam, 1f, 1f, 1_000).interval, 0.001f)
    }

    @Test
    fun lessonsLearnedUnderTheClassicSchedulerCarryOver() {
        // No stability stored: taken from the interval, the difficulty from the ease.
        val old = CardState(Phase.REVIEW, interval = 12f, ease = 2.5f, due = t0, seen = t0 - 12 * day, reps = 5)
        assertEquals(12f, Scheduler.memory(old)!!.s, 0.001f)
        assertEquals(0.9f, Scheduler.retrievability(old, t0)!!, 0.001f)
        val next = Scheduler.review(old, Rating.GOOD, t0, s)
        assertTrue("then ${next.interval}", next.interval in 20f..45f)
        assertTrue(next.stability > 12f && next.difficulty in 1f..10f)
        assertTrue(Memory.fromClassic(12f, 1.3f).d > Memory.fromClassic(12f, 2.5f).d) // low ease = hard
        assertNull(Scheduler.memory(CardState()))
        // In the middle of its learning steps: still a young memory.
        val learning = Scheduler.review(CardState(Phase.LEARNING, step = 1, seen = t0 - 10 * minute), Rating.GOOD, t0, s)
        assertEquals(Phase.REVIEW, learning.phase)
        assertTrue(learning.interval in 1f..3f)
        // And the classic scheduler still works as before for whoever prefers it.
        assertEquals(30f, Scheduler.review(old, Rating.GOOD, t0, s.copy(engine = "classic")).interval, 0.001f)
    }

    @Test
    fun newLessonsWaitWhileReviewsPileUp() {
        val reviews = (1..40).map { "r$it" }
        val keys = reviews + "new"
        val cards = reviews.associateWith { CardState(Phase.REVIEW, interval = 3f, due = t0 - day) }
        // 40 waiting against a goal of 30: reviews only, even when a new lesson's turn has come.
        assertEquals("r1", Scheduler.next(keys, cards, emptyList(), 0, 9, t0, s))
        assertEquals("new", Scheduler.next(keys, cards, emptyList(), 0, 9, t0, s.copy(holdNew = false)))
        assertEquals("new", Scheduler.next(keys, cards, emptyList(), 0, 9, t0, s.copy(dailyGoal = 50)))
    }

    @Test
    fun theSchedulerFitsItselfToALearnerWhoForgetsFasterOrSlower() {
        assertNull(Memory.personalFactor(Calibration()))
        assertEquals(1f, Memory.personalFactor(Calibration(100f, 90f, 90f))!!, 0.001f)
        assertTrue(Memory.personalFactor(Calibration(100f, 90f, 80f))!! < 0.7f)
        assertTrue(Memory.personalFactor(Calibration(100f, 90f, 95f))!! > 1.4f)

        // A made-up learner whose memories last only 0.7 (or 1.3) times as long as the model says,
        // reviewing every lesson when it is due. With no adjustment they would remember less
        // (more) than the 90% asked for; with it, they end up close to 90%.
        fun study(truth: Float, adaptive: Boolean): Pair<Float, Float> {
            val random = Random(7)
            val settings = s.copy(adaptive = adaptive, maxDays = 100_000f) // no ceiling: it would hide the forgetting
            var calibration = Calibration()
            var asked = 0; var remembered = 0
            val cards = Array(1_500) { CardState() }
            val clock = LongArray(cards.size) { t0 + it * minute }
            repeat(11) { round ->
                for (i in cards.indices) {
                    val before = cards[i]
                    val now = clock[i]
                    val waited = if (before.seen > 0) (now - before.seen).toFloat() / day else 0f
                    val known = Scheduler.memory(before)
                    val recalls = known == null || random.nextFloat() < Memory.retrievability(waited, known.s * truth)
                    val rating = if (recalls) Rating.GOOD else Rating.AGAIN
                    val personal = if (adaptive) Memory.personalFactor(calibration) ?: 1f else 1f
                    if (known != null && before.phase == Phase.REVIEW && waited >= Memory.SAME_DAY) {
                        calibration = calibration.plus(Memory.retrievability(waited, known.s), recalls)
                        if (round >= 5) { asked++; if (recalls) remembered++ }
                    }
                    cards[i] = Scheduler.review(before, rating, now, settings, 1f, personal, 0)
                    clock[i] = cards[i].due
                }
            }
            return remembered.toFloat() / asked to (Memory.personalFactor(calibration) ?: 1f)
        }
        val (forgetfulPlain, _) = study(truth = 0.7f, adaptive = false)
        val (forgetful, shrink) = study(truth = 0.7f, adaptive = true)
        val (sp0, _) = study(truth = 1.3f, adaptive = false); val (sp1, st1) = study(truth = 1.3f, adaptive = true)
        println("DEBUG forgetful plain=$forgetfulPlain adj=$forgetful k=$shrink | strong plain=$sp0 adj=$sp1 k=$st1")
        assertTrue("unadjusted $forgetfulPlain", forgetfulPlain < 0.875f)
        assertTrue("adjusted $forgetful", abs(forgetful - 0.9f) < 0.025f)
        assertTrue("factor $shrink", shrink in 0.6f..0.85f)

        val (strongPlain, _) = study(truth = 1.3f, adaptive = false)
        val (strong, stretch) = study(truth = 1.3f, adaptive = true)
        assertTrue("unadjusted $strongPlain", strongPlain > 0.915f)
        assertTrue("adjusted $strong", abs(strong - 0.9f) < 0.025f)
        assertTrue("factor $stretch", stretch in 1.15f..1.5f)
    }
}
