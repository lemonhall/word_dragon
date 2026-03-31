package me.lemonhall.worddragon.ui.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.lemonhall.worddragon.WordDragonDependencies
import me.lemonhall.worddragon.data.content.ChapterDefinition
import me.lemonhall.worddragon.data.progress.ProgressStore
import me.lemonhall.worddragon.domain.game.IdiomDefinition
import me.lemonhall.worddragon.domain.game.LevelDefinition
import me.lemonhall.worddragon.domain.game.LevelPlacement
import me.lemonhall.worddragon.domain.game.WordOrientation
import me.lemonhall.worddragon.testsupport.FakeErrorSoundPlayer
import me.lemonhall.worddragon.testsupport.FakeIdiomCatalogDataSource
import me.lemonhall.worddragon.testsupport.FakeLevelPackDataSource
import me.lemonhall.worddragon.testsupport.FakeTtsSpeaker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameViewModelSpeechFlowTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun autoAdvanceAfterManualCompletionSpeaksNextIdiomExplanation() {
        val speaker = FakeTtsSpeaker()
        val dependencies =
            createCrossIdiomDependencies(
                prefsName = "game-viewmodel-speech-manual",
                speaker = speaker,
            )
        dependencies.progressStore.clearAll()

        val viewModel = createViewModel(dependencies)

        viewModel.inputCandidate('高')
        viewModel.inputCandidate('山')
        viewModel.inputCandidate('流')
        viewModel.inputCandidate('水')

        assertEquals("idiom-2", viewModel.uiState.value.selectedIdiomId)
        assertEquals(
            listOf("比喻知音难遇。", "高山流水。", "条件成熟后自然成功。"),
            speaker.spokenTexts,
        )
    }

    @Test
    fun autoAdvanceAfterRevealWholeIdiomAlsoSpeaksNextIdiomExplanation() {
        val speaker = FakeTtsSpeaker()
        val dependencies =
            createCrossIdiomDependencies(
                prefsName = "game-viewmodel-speech-reveal",
                speaker = speaker,
            )
        dependencies.progressStore.clearAll()

        val viewModel = createViewModel(dependencies)

        viewModel.revealWholeIdiom()

        assertEquals("idiom-2", viewModel.uiState.value.selectedIdiomId)
        assertEquals(
            listOf("比喻知音难遇。", "高山流水。", "条件成熟后自然成功。"),
            speaker.spokenTexts,
        )
    }

    private fun createViewModel(dependencies: WordDragonDependencies): GameViewModel =
        GameViewModel(
            levelId = "level-0001",
            idiomCatalogDataSource = dependencies.idiomCatalogDataSource,
            levelPackDataSource = dependencies.levelPackDataSource,
            progressStore = dependencies.progressStore,
            ttsSpeaker = dependencies.ttsSpeaker,
            errorSoundPlayer = dependencies.errorSoundPlayer,
        )

    private fun createCrossIdiomDependencies(
        prefsName: String,
        speaker: FakeTtsSpeaker,
    ): WordDragonDependencies {
        val idioms =
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
        val level =
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
        val chapter =
            ChapterDefinition(
                chapterId = "chapter-001",
                title = "第一章 常用成语",
                levelIds = listOf("level-0001"),
                levelCount = 1,
                firstLevelId = "level-0001",
            )
        return WordDragonDependencies(
            idiomCatalogDataSource = FakeIdiomCatalogDataSource(idioms.associateBy { it.id }),
            levelPackDataSource = FakeLevelPackDataSource(chapters = listOf(chapter), levels = mapOf(level.levelId to level)),
            progressStore = ProgressStore(context, prefsName = prefsName),
            ttsSpeaker = speaker,
            errorSoundPlayer = FakeErrorSoundPlayer(),
        )
    }
}
