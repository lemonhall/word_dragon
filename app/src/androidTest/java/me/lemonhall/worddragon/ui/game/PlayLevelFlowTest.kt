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
import me.lemonhall.worddragon.domain.game.HintUsage
import me.lemonhall.worddragon.domain.game.LevelProgressSnapshot
import me.lemonhall.worddragon.testsupport.FakeErrorSoundPlayer
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
    fun continueGameUsesBoardDrivenInputAndSpeaksCompletedIdiomAndUsesCompactLabels() {
        val speaker = FakeTtsSpeaker()
        val errorSoundPlayer = FakeErrorSoundPlayer()
        val dependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = "play-level-flow",
                speaker = speaker,
                errorSoundPlayer = errorSoundPlayer,
            )
        dependencies.progressStore.clearAll()

        composeRule.setContent {
            WordDragonApp(dependencies = dependencies)
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("game-screen").assertIsDisplayed()
        composeRule.onNodeWithText("第0001级").assertIsDisplayed()
        composeRule.onAllNodesWithText("关卡练习").assertCountEquals(0)
        composeRule.onAllNodesWithText("当前成语").assertCountEquals(0)
        composeRule.onAllNodesWithText("成语列表").assertCountEquals(0)
        composeRule.onAllNodesWithText("释义提示").assertCountEquals(0)
        composeRule.onAllNodesWithText("候选字盘").assertCountEquals(0)
        composeRule.onAllNodesWithText("chapter-001 / level-0001").assertCountEquals(0)

        composeRule.onNodeWithTag("cell-0-0").performClick()
        composeRule.onNodeWithTag("candidate-水").performClick()
        composeRule.onNodeWithTag("cell-0-0").assertTextEquals("")
        composeRule.runOnIdle {
            assertEquals(1, errorSoundPlayer.rejectCount)
        }

        composeRule.onNodeWithTag("candidate-高").performClick()
        composeRule.onNodeWithTag("candidate-高").assertIsNotEnabled()
        composeRule.onNodeWithTag("candidate-山").performClick()
        composeRule.onNodeWithTag("candidate-流").performClick()
        composeRule.onNodeWithTag("candidate-流").assertIsNotEnabled()
        composeRule.onNodeWithTag("candidate-水").performClick()
        composeRule.runOnIdle {
            val progress = dependencies.progressStore.readProgress()
            assertEquals("level-0002", progress.currentLevelId)
        }

        composeRule.onAllNodesWithTag("game-complete-card").assertCountEquals(1)
        composeRule.onAllNodesWithTag("action-next-level").assertCountEquals(1)
        assertTrue(speaker.spokenTexts.isNotEmpty())
        assertTrue(speaker.spokenTexts.count { it == "高山流水。" } == 1)
        assertTrue(speaker.spokenTexts.contains("比喻知音难遇。"))
        assertTrue(speaker.spokenTexts.filter { it.contains("高山流水") } == listOf("高山流水。"))
    }

    @Test
    fun revealSingleCharSpeaksCompletedIdiomWhenItFinishesTheWord() {
        val speaker = FakeTtsSpeaker()
        val dependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = "play-level-flow-hint-char",
                speaker = speaker,
            )
        dependencies.progressStore.clearAll()
        dependencies.progressStore.saveSnapshot(
            LevelProgressSnapshot(
                levelId = "level-0001",
                selectedIdiomId = "idiom-1",
                focusedCellKey = "0,3",
                cellInputs = mapOf("0,0" to "高", "0,1" to "山", "0,2" to "流"),
                hintUsage = HintUsage(),
                isCompleted = false,
            ),
        )

        composeRule.setContent {
            WordDragonApp(dependencies = dependencies)
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("action-hint-char").performClick()

        composeRule.runOnIdle {
            assertTrue(speaker.spokenTexts.contains("高山流水。"))
        }
    }

    @Test
    fun revealWholeIdiomSpeaksCompletedIdiom() {
        val speaker = FakeTtsSpeaker()
        val dependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = "play-level-flow-hint-idiom",
                speaker = speaker,
            )
        dependencies.progressStore.clearAll()

        composeRule.setContent {
            WordDragonApp(dependencies = dependencies)
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("action-hint-idiom").performClick()

        composeRule.runOnIdle {
            assertTrue(speaker.spokenTexts.contains("高山流水。"))
        }
    }
}
