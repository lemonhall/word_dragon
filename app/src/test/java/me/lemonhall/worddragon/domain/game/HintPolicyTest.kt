package me.lemonhall.worddragon.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintPolicyTest {
    @Test
    fun revealSingleCharTargetsFirstUnfilledOrIncorrectCell() {
        val reveal = HintPolicy.revealSingleChar(solution = "高山流水", current = "高山流口")

        assertEquals(3, reveal?.index)
        assertEquals('水', reveal?.char)
    }

    @Test
    fun revealSingleCharReturnsNullForSolvedIdiom() {
        val reveal = HintPolicy.revealSingleChar(solution = "高山流水", current = "高山流水")

        assertNull(reveal)
    }

    @Test
    fun revealWholeIdiomReturnsFullSolution() {
        val reveal = HintPolicy.revealWholeIdiom(solution = "高山流水", current = "高山流口")

        assertEquals("高山流水", reveal.text)
        assertEquals(1, reveal.changedCellCount)
    }
}
