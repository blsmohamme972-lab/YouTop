package app.youtop

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.youtop.study.Phase
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real activity through its main screens and saves a picture of each
 * to app/build/screens, so layout changes can be looked at without a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class ScreensTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1200)
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png")
    }

    /** The feed keeps neighbouring pages composed, so pick the match that is actually on screen. */
    private fun visible(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val height = rule.onAllNodes(isRoot())[0].fetchSemanticsNode().size.height
        val all = rule.onAllNodes(matcher)
        val index = all.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.width > 0 && it.boundsInRoot.top >= 0 && it.boundsInRoot.bottom <= height }
        return all[index]
    }

    /** Waits for work that hops between background threads and the main thread (e.g. saving settings). */
    private fun settle(condition: () -> Boolean) {
        repeat(200) {
            rule.waitForIdle()
            if (condition()) return
            Thread.sleep(25)
        }
        error("Condition not reached")
    }

    private fun button(label: String) = visible(hasContentDescription(label))
    private fun text(text: String) = visible(hasText(text))

    private fun openFirstLesson() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("كيف يسافر الضوء").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun seekToSentence(index: Int) = rule.runOnUiThread { rule.activity.viewModel().narrator.seekToSegment(index) }

    @Test
    fun feedAndSpotlight() {
        openFirstLesson()
        shot("1-feed")
        seekToSentence(3) // a sentence that points at a region of the picture
        shot("2-spotlight")
        text("Explain").performClick()
        seekToSentence(6)
        shot("3-explanation")
    }

    @Test
    fun transcript() {
        openFirstLesson()
        seekToSentence(4)
        button("Text").performClick()
        shot("4-transcript")
    }

    @Test
    fun quiz() {
        openFirstLesson()
        button("Quiz").performClick()
        shot("5-quiz")
        text("In circles").performClick()
        shot("5b-quiz-answered")
        // Moving on is what feeds the scheduler: a wrong answer puts the lesson in its first learning step.
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        val vm = rule.activity.viewModel()
        val card = vm.progress.value.cards.getValue("starter/examples/how-light-travels")
        assertEquals(Phase.LEARNING, card.phase)
        assertEquals(1, vm.progress.value.doneToday)
    }

    @Test
    fun secondLessonAndLibrary() {
        openFirstLesson()
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { rule.activity.viewModel().queue.size >= 3 }
        button("Save").performClick()
        shot("6-second-lesson")
        button("Library").performClick()
        shot("7-library")
        text("YouTop Starter").performClick()
        shot("7b-book")
        text("Topics").performClick()
        shot("7c-topics")
        text("Cardiac surgery").performClick() // focus the feed on cardiac surgery
        settle { rule.activity.viewModel().settings.value.study.focus == setOf("cardiac") }
        text("Show all").performClick()
        settle { rule.activity.viewModel().settings.value.study.focus.isEmpty() }
        text("Study").performClick()
        shot("8-study")
        rule.onNode(hasText("Advanced timing")).performScrollTo().performClick()
        rule.onNode(hasText("Longest interval (days)")).performScrollTo()
        shot("8b-study-advanced")
        text("Sync").performClick()
        shot("9-sync")
    }

    @Test
    fun questionFirstSkipsTheVideoUntilAsked() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        rule.runOnUiThread { vm.setStudy(vm.settings.value.study.copy(questionFirst = true)) }
        settle { vm.settings.value.study.questionFirst }
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasText("Which brain structure", substring = true)).fetchSemanticsNodes().any { it.boundsInRoot.width > 0 }
        }
        shot("10-question-first")
        assertEquals(false, vm.narrator.state.value.playing)

        text("Hippocampus").performClick()
        rule.waitForIdle()
        text("Watch").performClick()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(600)
        button("Quiz").assertExists() // back on the lesson itself
        assertEquals(true, vm.narrator.state.value.playing)
    }
}
