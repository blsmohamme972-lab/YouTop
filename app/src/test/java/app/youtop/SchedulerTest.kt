package app.youtop

import app.youtop.study.CardState
import app.youtop.study.Phase
import app.youtop.study.Rating
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    /** The classic ease-factor rules; smart scheduling has its own tests below. */
    private val s = StudySettings(smart = false)
    private val smart = StudySettings()
    private val minute = 60_000L
    private val day = 86_400_000L
    private val t0 = 1_000 * day

    @Test
    fun newLessonWalksThroughLearningStepsThenGraduates() {
        var c = Scheduler.review(CardState(), Rating.GOOD, t0, s)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + 10 * minute, c.due)
        c = Scheduler.review(c, Rating.GOOD, c.due, s)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(1f, c.interval, 0.001f)
    }

    @Test
    fun wrongAnswerBringsANewLessonBackInAMinute() {
        val c = Scheduler.review(CardState(), Rating.AGAIN, t0, s)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + minute, c.due)
    }

    @Test
    fun easyAnswerSkipsLearning() {
        val c = Scheduler.review(CardState(), Rating.EASY, t0, s)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(4f, c.interval, 0.001f)
    }

    @Test
    fun intervalsGrowWithEachCorrectReview() {
        var c = CardState(phase = Phase.REVIEW, interval = 1f, due = t0)
        val seen = mutableListOf<Float>()
        repeat(4) {
            c = Scheduler.review(c, Rating.GOOD, c.due, s)
            seen += c.interval
        }
        assertEquals(listOf(2.5f, 6.25f, 15.625f, 39.0625f), seen)
    }

    @Test
    fun lapseResetsIntervalLowersEaseAndRelearns() {
        val c = Scheduler.review(CardState(phase = Phase.REVIEW, interval = 20f, due = t0), Rating.AGAIN, t0, s)
        assertEquals(Phase.RELEARNING, c.phase)
        assertEquals(1f, c.interval, 0.001f)
        assertEquals(2.3f, c.ease, 0.001f)
        assertEquals(1, c.lapses)
        assertEquals(t0 + 10 * minute, c.due)
        val back = Scheduler.review(c, Rating.GOOD, c.due, s)
        assertEquals(Phase.REVIEW, back.phase)
    }

    @Test
    fun intervalNeverExceedsTheMaximum() {
        val c = Scheduler.review(CardState(phase = Phase.REVIEW, interval = 300f, due = t0), Rating.EASY, t0, s)
        assertEquals(365f, c.interval, 0.001f)
    }

    @Test
    fun reviewingAheadOfTimeDoesNotInflateTheInterval() {
        val card = CardState(phase = Phase.REVIEW, interval = 10f, due = t0 + 9 * day)
        val c = Scheduler.review(card, Rating.GOOD, t0, s) // only one day after the last review
        assertEquals(11.5f, c.interval, 0.001f) // a tenth of the wait earns a tenth of the growth
        // A hard answer given early must not earn more than a hard answer given on time.
        val almost = CardState(phase = Phase.REVIEW, interval = 10f, due = t0 + 2 * day)
        val early = Scheduler.review(almost, Rating.HARD, t0, s).interval
        val onTime = Scheduler.review(almost.copy(due = t0), Rating.HARD, t0, s).interval
        assertTrue("$early <= $onTime", early <= onTime)
    }

    @Test
    fun aReviewOnItsDueDayCountsAsOnTime() {
        // Studied at 22:00 with one day to wait, reviewed at 08:00 the next morning.
        val card = CardState(phase = Phase.REVIEW, interval = 1f, due = t0 + 14 * 3_600_000L, seen = t0 - 10 * 3_600_000L)
        assertEquals(2.5f, Scheduler.review(card, Rating.GOOD, t0, s).interval, 0.01f)
        val onTheMinute = Scheduler.review(card.copy(stability = 1f, difficulty = 5f), Rating.GOOD, card.due, smart).interval
        val thatMorning = Scheduler.review(card.copy(stability = 1f, difficulty = 5f), Rating.GOOD, t0, smart).interval
        assertEquals(onTheMinute, thatMorning, 0.01f)
    }

    // ---- smart scheduling (memory model) ----

    @Test
    fun smartNewLessonStillWalksTheLearningStepsThenWaitsDays() {
        var c = Scheduler.review(CardState(), Rating.GOOD, t0, smart)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + 10 * minute, c.due)
        c = Scheduler.review(c, Rating.GOOD, c.due, smart)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(3.7f, c.interval, 0.1f)
        // Known values of the model: about 4 days, then about 14, each right answer on time.
        c = Scheduler.review(c, Rating.GOOD, c.due, smart)
        assertEquals(14f, c.interval, 0.6f)
        val third = Scheduler.review(c, Rating.GOOD, c.due, smart)
        assertTrue(third.interval > 30f && third.interval < 60f)
    }

    @Test
    fun smartHardGoodEasyAreOrderedAndAWrongAnswerShrinks() {
        val card = CardState(phase = Phase.REVIEW, interval = 10f, due = t0, seen = t0 - 10 * day, stability = 10f, difficulty = 5f)
        val hard = Scheduler.review(card, Rating.HARD, t0, smart)
        val good = Scheduler.review(card, Rating.GOOD, t0, smart)
        val easy = Scheduler.review(card, Rating.EASY, t0, smart)
        assertTrue(hard.interval > 10f && hard.interval < good.interval && good.interval < easy.interval)
        assertTrue(hard.difficulty > good.difficulty && good.difficulty > easy.difficulty)

        val again = Scheduler.review(card, Rating.AGAIN, t0, smart)
        assertEquals(Phase.RELEARNING, again.phase)
        assertEquals(1, again.lapses)
        assertTrue(again.stability < 10f && again.difficulty > 5f)
        val back = Scheduler.review(again, Rating.GOOD, again.due, smart)
        assertEquals(Phase.REVIEW, back.phase)
        assertTrue("comes back sooner than before the lapse", back.interval in 1f..5f)
    }

    @Test
    fun smartHigherTargetMeansShorterWaits() {
        val card = CardState(phase = Phase.REVIEW, interval = 10f, due = t0, seen = t0 - 10 * day, stability = 10f, difficulty = 5f)
        val relaxed = Scheduler.review(card, Rating.GOOD, t0, smart.copy(retention = 0.85f)).interval
        val normal = Scheduler.review(card, Rating.GOOD, t0, smart).interval
        val strict = Scheduler.review(card, Rating.GOOD, t0, smart.copy(retention = 0.95f)).interval
        assertTrue(relaxed > normal && normal > strict)
        assertEquals(365f, Scheduler.review(card.copy(stability = 5_000f), Rating.EASY, t0, smart).interval, 0.001f)
    }

    @Test
    fun smartTakesOverALessonStudiedUnderTheClassicRules() {
        // Stored before smart scheduling existed: no stability, no difficulty, no "seen".
        val old = CardState(phase = Phase.REVIEW, interval = 20f, ease = 2.5f, due = t0, reps = 6)
        val c = Scheduler.review(old, Rating.GOOD, t0, smart)
        assertTrue(c.stability > 20f && c.difficulty in 1f..10f)
        assertTrue("grows, but not wildly: ${c.interval}", c.interval > 30f && c.interval < 90f)
        // Reviewed long before it was due, hardly anything was at risk: hardly any growth.
        val early = Scheduler.review(old.copy(due = t0 + 19 * day), Rating.GOOD, t0, smart)
        assertTrue(early.interval < 25f)
    }

    @Test
    fun smartDoesNotTrustAQuickFirstAnswerWithWeeks() {
        val c = Scheduler.review(CardState(), Rating.EASY, t0, smart)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(4f, c.interval, 0.001f)
    }

    @Test
    fun aLearnedLessonIsDueOnItsDayNotAtItsMinute() {
        // Studied at 22:00 yesterday with one day to wait; today at 15:00 it is ready.
        val threePm = t0 + 15 * 3_600_000L
        val dayEnd = Scheduler.dayEnd(threePm, 0)
        assertEquals(t0 + day + 4 * 3_600_000L, dayEnd)
        val cards = mapOf(
            "a" to CardState(phase = Phase.REVIEW, interval = 1f, due = t0 + 22 * 3_600_000L),
            "b" to CardState(phase = Phase.REVIEW, interval = 1f, due = t0 + day + 5 * 3_600_000L), // tomorrow
        )
        val keys = listOf("a", "b")
        assertEquals("a", Scheduler.next(keys, cards, emptyList(), 0, 0, threePm, s.copy(afterGoal = "stop"), dayEnd = dayEnd))
        assertNull(Scheduler.next(keys, cards, listOf("a"), 0, 0, threePm, s.copy(afterGoal = "stop"), dayEnd = dayEnd))
        assertTrue(Scheduler.isDue(cards.getValue("a"), threePm, dayEnd))
        // A lesson still in its learning steps keeps to the minute.
        assertTrue(!Scheduler.isDue(CardState(Phase.LEARNING, due = threePm + minute), threePm, dayEnd))
    }

    @Test
    fun theMostOverdueForItsIntervalComesFirst() {
        val cards = mapOf(
            "long" to CardState(phase = Phase.REVIEW, interval = 60f, due = t0 - 3 * day),
            "short" to CardState(phase = Phase.REVIEW, interval = 2f, due = t0 - 2 * day),
        )
        assertEquals("short", Scheduler.next(listOf("long", "short"), cards, emptyList(), 0, 0, t0, s))
    }

    @Test
    fun feedPrefersDueLearningThenMixesNewIntoReviews() {
        val keys = listOf("a", "b", "c", "d")
        val cards = mapOf(
            "a" to CardState(phase = Phase.REVIEW, interval = 3f, due = t0 - day),
            "b" to CardState(phase = Phase.LEARNING, due = t0 - minute),
        )
        assertEquals("b", Scheduler.next(keys, cards, emptyList(), 0, 0, t0, s))
        // "b" just shown: a review is due and no review has been shown since the last new lesson.
        assertEquals("a", Scheduler.next(keys, cards, listOf("b"), 0, 0, t0, s))
        // After enough reviews a new lesson is slipped in, in book order.
        assertEquals("c", Scheduler.next(keys, cards, listOf("b"), 0, 3, t0, s))
    }

    @Test
    fun feedNeverRepeatsALessonStraightAwayAndNeverRunsDryByDefault() {
        val keys = listOf("a", "b")
        val cards = keys.associateWith { CardState(phase = Phase.REVIEW, interval = 5f, due = t0 + 5 * day) }
        assertEquals("b", Scheduler.next(keys, cards, listOf("a"), 0, 0, t0, s))
        assertNull(Scheduler.next(keys, cards, listOf("a"), 0, 0, t0, s.copy(afterGoal = "stop")))
        assertEquals("a", Scheduler.next(listOf("a"), cards, listOf("a"), 0, 0, t0, s))
    }

    @Test
    fun dailyLimitHoldsBackNewLessonsWhileReviewsAreDue() {
        val keys = listOf("a", "b")
        val cards = mapOf("a" to CardState(phase = Phase.REVIEW, interval = 3f, due = t0 - day))
        assertEquals("a", Scheduler.next(keys, cards, emptyList(), newToday = 20, sinceNew = 9, now = t0, s = s))
    }

    @Test
    fun behaviourBecomesARating() {
        assertEquals(Rating.GOOD, Scheduler.rate(1, 0, 1, 9_000, true, 60_000))
        assertEquals(Rating.EASY, Scheduler.rate(1, 0, 1, 2_000, false, 5_000))
        assertEquals(Rating.AGAIN, Scheduler.rate(1, 1, 1, 2_000, true, 60_000))
        assertEquals(Rating.HARD, Scheduler.rate(3, 1, 3, 5_000, true, 60_000))
        assertEquals(Rating.HARD, Scheduler.rate(0, 0, 1, 0, true, 60_000))
        assertEquals(Rating.GOOD, Scheduler.rate(0, 0, 0, 0, true, 60_000))
        assertNull(Scheduler.rate(0, 0, 1, 0, false, 2_000))
        assertNull(Scheduler.rate(0, 0, 1, 0, false, 30_000))
    }

    @Test
    fun choosingATagNarrowsOrWidensSensibly() {
        assertEquals(setOf("cardiac"), app.youtop.study.Tags.toggle(emptySet(), "cardiac"))
        // A subtopic replaces its chosen parent; a parent swallows its chosen subtopics.
        assertEquals(setOf("d:anatomy", "cardiac/cpb"), app.youtop.study.Tags.toggle(setOf("cardiac", "d:anatomy"), "cardiac/cpb"))
        assertEquals(setOf("thoracic", "cardiac"), app.youtop.study.Tags.toggle(setOf("cardiac/cpb", "cardiac/aorta", "thoracic"), "cardiac"))
        assertEquals(emptySet<String>(), app.youtop.study.Tags.toggle(setOf("cardiac"), "cardiac"))
    }

    @Test
    fun studyDayRollsOverAtFourInTheMorning() {
        val threeAm = t0 + 3 * 3_600_000L
        val fiveAm = t0 + 5 * 3_600_000L
        assertTrue(Scheduler.dayOf(fiveAm, 0) == Scheduler.dayOf(threeAm, 0) + 1)
    }
}
