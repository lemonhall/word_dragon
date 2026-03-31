package me.lemonhall.worddragon.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.lemonhall.worddragon.WordDragonApp
import me.lemonhall.worddragon.testsupport.FakeTtsSpeaker
import me.lemonhall.worddragon.testsupport.buildFakeDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayLevelFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun continueGameUsesBoardDrivenInputAndDoesNotSpeakAnswer() {
        val speaker = FakeTtsSpeaker()
        val dependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = "play-level-flow",
                speaker = speaker,
            )
        dependencies.progressStore.clearAll()

        composeRule.setContent {
            WordDragonApp(dependencies = dependencies)
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("game-screen").assertIsDisplayed()
        composeRule.onAllNodesWithText("当前成语").assertCountEquals(0)
        composeRule.onAllNodesWithText("成语列表").assertCountEquals(0)

        composeRule.onNodeWithTag("cell-0-2").performClick()
        composeRule.onNodeWithTag("candidate-流").performClick()
        composeRule.onNodeWithTag("cell-0-2").assertTextEquals("流")
        composeRule.onNodeWithTag("candidate-流").assertIsNotEnabled()

        composeRule.onNodeWithTag("cell-0-0").performClick()
        composeRule.onNodeWithTag("candidate-高").performClick()
        composeRule.onNodeWithTag("candidate-高").assertIsNotEnabled()
        composeRule.onNodeWithTag("candidate-山").performClick()
        composeRule.onNodeWithTag("candidate-水").performClick()
        composeRule.runOnIdle {
            val progress = dependencies.progressStore.readProgress()
            assertEquals("level-0002", progress.currentLevelId)
        }

        composeRule.onAllNodesWithTag("game-complete-card").assertCountEquals(1)
        composeRule.onAllNodesWithTag("action-next-level").assertCountEquals(1)
        assertTrue(speaker.spokenTexts.isNotEmpty())
        assertTrue(speaker.spokenTexts.all { it == "比喻知音难遇。" })
        assertTrue(speaker.spokenTexts.none { it.contains("高山流水") })
    }
}
