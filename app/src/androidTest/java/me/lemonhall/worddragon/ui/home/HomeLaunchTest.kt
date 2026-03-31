package me.lemonhall.worddragon.ui.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.lemonhall.worddragon.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsPrimaryEntryPoints() {
        composeRule.onNodeWithText("继续游戏").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("章节选关").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("设置").assertIsDisplayed().assertHasClickAction()
    }
}
