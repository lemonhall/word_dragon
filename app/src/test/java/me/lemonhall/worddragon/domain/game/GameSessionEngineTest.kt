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
        assertFalse(state.isCompleted)

        state = engine.inputCandidate(state, '高')
        state = engine.inputCandidate(state, '山')
        state = engine.inputCandidate(state, '流')
        state = engine.inputCandidate(state, '水')

        state = engine.selectIdiom(state, "idiom-2")
        state = engine.inputCandidate(state, '到')
        state = engine.inputCandidate(state, '渠')
        state = engine.inputCandidate(state, '成')

        assertTrue(state.isCompleted)
        assertTrue(state.idiomStates.all { it.isSolved })
        assertEquals("高山流水。比喻知音难遇。", state.currentSpeechText)
    }

    @Test
    fun reusesFirstIncorrectCellSoPlayerCanFixMistakes() {
        val engine = GameSessionEngine(sampleLevel(), sampleIdioms())

        var state = engine.startSession()
        state = engine.inputCandidate(state, '错')
        state = engine.inputCandidate(state, '山')
        state = engine.inputCandidate(state, '流')
        state = engine.inputCandidate(state, '水')

        assertFalse(state.idiomStates.first().isSolved)

        state = engine.inputCandidate(state, '高')

        assertEquals("高山流水", state.idiomStates.first().filledText)
        assertTrue(state.idiomStates.first().isSolved)
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
}
