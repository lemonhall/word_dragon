package me.lemonhall.worddragon.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionEngineTest {
    @Test
    fun fillsCrosswordFromCandidateTrayAndCompletesLevel() {
        val engine = GameSessionEngine(sampleLevel(), sampleIdioms())

        var state = engine.startSession()
        assertEquals("idiom-1", state.selectedIdiomId)
        assertEquals(CellCoordinate(row = 0, col = 0), state.focusedCellCoordinate)
        assertFalse(state.isCompleted)

        state = engine.inputCandidate(state, '高')
        state = engine.inputCandidate(state, '山')
        state = engine.inputCandidate(state, '流')
        state = engine.inputCandidate(state, '水')
        assertEquals("idiom-2", state.selectedIdiomId)
        assertEquals(CellCoordinate(row = 1, col = 3), state.focusedCellCoordinate)
        assertEquals("已完成一条。", state.currentSpeechText)

        state = engine.inputCandidate(state, '到')
        state = engine.inputCandidate(state, '渠')
        state = engine.inputCandidate(state, '成')

        assertTrue(state.isCompleted)
        assertTrue(state.idiomStates.all { it.isSolved })
        assertEquals("本关完成，可以进入下一关。", state.currentSpeechText)
    }

    @Test
    fun rejectsIncorrectCandidateWithoutConsumingInventory() {
        val engine = GameSessionEngine(sampleLevel(), sampleIdioms())

        var state = engine.startSession()
        state = engine.inputCandidate(state, '水')

        assertEquals(CellCoordinate(row = 0, col = 0), state.focusedCellCoordinate)
        assertTrue(state.cellInputs.isEmpty())
        assertEquals(1, state.candidateStates.first { it.char == '水' }.remainingCount)
        assertEquals(InputFeedback.REJECTED, state.lastInputFeedback)

        state = engine.inputCandidate(state, '高')

        assertEquals(CellCoordinate(row = 0, col = 1), state.focusedCellCoordinate)
        assertEquals(InputFeedback.NONE, state.lastInputFeedback)
    }

    @Test
    fun clickingSharedCellTwiceTogglesBetweenAcrossAndDown() {
        val engine = GameSessionEngine(sampleLevel(), sampleIdioms())

        var state = engine.startSession()

        state = engine.selectCell(state, CellCoordinate(row = 0, col = 3))
        assertEquals("idiom-1", state.selectedIdiomId)
        assertEquals(CellCoordinate(row = 0, col = 3), state.focusedCellCoordinate)

        state = engine.selectCell(state, CellCoordinate(row = 0, col = 3))
        assertEquals("idiom-2", state.selectedIdiomId)
        assertEquals(CellCoordinate(row = 0, col = 3), state.focusedCellCoordinate)
        assertEquals("条件成熟后自然成功。", state.currentSpeechText)
    }

    @Test
    fun restoresFocusedCellFromSnapshot() {
        val engine = GameSessionEngine(sampleLevel(), sampleIdioms())

        val state =
            engine.startSession(
                snapshot =
                    LevelProgressSnapshot(
                        levelId = "level-0001",
                        selectedIdiomId = "idiom-2",
                        focusedCellKey = "2,3",
                        cellInputs = mapOf("0,3" to "水", "1,3" to "到"),
                    ),
            )

        assertEquals("idiom-2", state.selectedIdiomId)
        assertEquals(CellCoordinate(row = 2, col = 3), state.focusedCellCoordinate)
        assertEquals("条件成熟后自然成功。", state.currentSpeechText)
    }

    @Test
    fun candidateInventoryOnlyConsumesCorrectPlacements() {
        val engine = GameSessionEngine(duplicateCandidateLevel(), duplicateCandidateIdioms())

        var state = engine.startSession()
        assertEquals(2, state.candidateStates.first { it.char == '哈' }.remainingCount)
        assertEquals(1, state.candidateStates.first { it.char == '大' }.remainingCount)

        state = engine.inputCandidate(state, '大')
        assertEquals(1, state.candidateStates.first { it.char == '大' }.remainingCount)
        assertEquals(2, state.candidateStates.first { it.char == '哈' }.remainingCount)
        assertEquals(InputFeedback.REJECTED, state.lastInputFeedback)

        state = engine.inputCandidate(state, '哈')
        state = engine.inputCandidate(state, '哈')
        assertEquals(CellCoordinate(row = 0, col = 2), state.focusedCellCoordinate)

        assertEquals(1, state.candidateStates.first { it.char == '大' }.remainingCount)
        assertEquals(0, state.candidateStates.first { it.char == '哈' }.remainingCount)
        assertFalse(state.candidateStates.first { it.char == '哈' }.isEnabled)

        state = engine.inputCandidate(state, '大')
        assertEquals(0, state.candidateStates.first { it.char == '大' }.remainingCount)
        assertFalse(state.candidateStates.first { it.char == '大' }.isEnabled)
    }

    private fun sampleLevel() =
        LevelDefinition(
            levelId = "level-0001",
            chapterId = "chapter-001",
            idiomIds = listOf("idiom-1", "idiom-2"),
            boardWidth = 7,
            boardHeight = 4,
            candidateChars = listOf('高', '山', '流', '水', '到', '渠', '成'),
            placements =
                listOf(
                    LevelPlacement(
                        idiomId = "idiom-1",
                        orientation = WordOrientation.ACROSS,
                        row = 0,
                        col = 0,
                    ),
                    LevelPlacement(
                        idiomId = "idiom-2",
                        orientation = WordOrientation.DOWN,
                        row = 0,
                        col = 3,
                    ),
                ),
        )

    private fun sampleIdioms() =
        listOf(
            IdiomDefinition(
                id = "idiom-1",
                text = "高山流水",
                shortExplanation = "比喻知音难遇。",
                ttsText = "高山流水。比喻知音难遇。",
            ),
            IdiomDefinition(
                id = "idiom-2",
                text = "水到渠成",
                shortExplanation = "条件成熟后自然成功。",
                ttsText = "水到渠成。条件成熟后自然成功。",
            ),
        )

    private fun duplicateCandidateLevel() =
        LevelDefinition(
            levelId = "level-dup",
            chapterId = "chapter-001",
            idiomIds = listOf("idiom-dup"),
            boardWidth = 4,
            boardHeight = 1,
            candidateChars = listOf('哈', '哈', '大', '笑'),
            placements =
                listOf(
                    LevelPlacement(
                        idiomId = "idiom-dup",
                        orientation = WordOrientation.ACROSS,
                        row = 0,
                        col = 0,
                    ),
                ),
        )

    private fun duplicateCandidateIdioms() =
        listOf(
            IdiomDefinition(
                id = "idiom-dup",
                text = "哈哈大笑",
                shortExplanation = "形容非常高兴。",
                ttsText = "哈哈大笑。形容非常高兴。",
            ),
        )
}
