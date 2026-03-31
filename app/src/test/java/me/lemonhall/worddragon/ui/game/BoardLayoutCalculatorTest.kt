package me.lemonhall.worddragon.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayoutCalculatorTest {
    @Test
    fun sevenBySevenBoardFitsTypicalPortraitCardWidth() {
        val layout = BoardLayoutCalculator.calculate(availableWidthDp = 312, boardWidth = 7)

        assertEquals(12, layout.horizontalPaddingDp)
        assertEquals(4, layout.cellSpacingDp)
        assertTrue(layout.cellSizeDp >= 34)
        assertTrue(layout.requiredWidthDp(boardWidth = 7) <= 312)
        assertFalse(layout.requiresHorizontalScroll)
    }

    @Test
    fun narrowBoardFallsBackToHorizontalScrollInsteadOfClipping() {
        val layout = BoardLayoutCalculator.calculate(availableWidthDp = 260, boardWidth = 7)

        assertTrue(layout.requiresHorizontalScroll)
        assertTrue(layout.requiredWidthDp(boardWidth = 7) > 260)
    }
}
