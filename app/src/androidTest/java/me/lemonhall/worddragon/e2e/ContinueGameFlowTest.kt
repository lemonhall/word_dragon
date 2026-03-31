package me.lemonhall.worddragon.e2e

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.lemonhall.worddragon.WordDragonApp
import me.lemonhall.worddragon.testsupport.buildFakeDependencies
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueGameFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun continueGameRestoresBoardSnapshotAfterRelaunch() {
        val prefsName = "continue-flow"
        val firstDependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = prefsName,
            )
        firstDependencies.progressStore.clearAll()
        val secondDependencies =
            buildFakeDependencies(
                context = composeRule.activity,
                prefsName = prefsName,
            )
        var appInstance by mutableIntStateOf(0)

        composeRule.setContent {
            key(appInstance) {
                WordDragonApp(
                    dependencies = if (appInstance == 0) firstDependencies else secondDependencies,
                )
            }
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("candidate-高").performClick()
        composeRule.onNodeWithTag("candidate-山").performClick()
        composeRule.onNodeWithTag("cell-0-0").assertTextEquals("高")
        composeRule.onNodeWithTag("cell-0-1").assertTextEquals("山")

        composeRule.runOnUiThread {
            appInstance = 1
        }

        composeRule.onNodeWithText("继续游戏").performClick()
        composeRule.onNodeWithTag("game-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("cell-0-0").assertTextEquals("高")
        composeRule.onNodeWithTag("cell-0-1").assertTextEquals("山")
        composeRule.onNodeWithTag("selected-idiom-card").assertIsDisplayed()
    }
}
