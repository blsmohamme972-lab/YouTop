package app.youtop

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
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
        rule.waitForIdle()
        text("In circles").performClick()
        shot("5-quiz")
    }

    @Test
    fun secondLessonAndLibrary() {
        openFirstLesson()
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("لماذا نتذكّر القصص؟").fetchSemanticsNodes().isNotEmpty() }
        button("Save").performClick()
        shot("6-second-lesson")
        button("Library").performClick()
        shot("7-library")
        text("Sync").performClick()
        shot("8-sync")
    }
}
