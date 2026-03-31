package me.lemonhall.worddragon.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class WordDragonTypographyTest {
    @Test
    fun boardAndCandidateTokensRespectSeniorMinimums() {
        assertTrue(WordDragonDimensions.MinTouchTargetDp >= 56)
        assertTrue(WordDragonDimensions.MinBoardCellSp >= 28)
    }
}

