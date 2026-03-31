package me.lemonhall.worddragon.e2e

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.lemonhall.worddragon.WordDragonDependencies
import me.lemonhall.worddragon.testsupport.buildFakeDependencies
import me.lemonhall.worddragon.ui.game.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RotateAndResumeTest {
    @Test
    fun restoreAfterRotationKeepsGameScreenAndBoardProgress() {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstDependencies =
            buildFakeDependencies(
                context = appContext,
                prefsName = "rotate-resume",
            )
        firstDependencies.progressStore.clearAll()

        val firstViewModel = newGameViewModel(firstDependencies)
        firstViewModel.selectCell(row = 0, col = 1)
        firstViewModel.inputCandidate('山')
        firstViewModel.selectCell(row = 0, col = 2)

        val savedSnapshot = firstDependencies.progressStore.readProgress().snapshots["level-0001"]
        requireNotNull(savedSnapshot)
        assertEquals("山", savedSnapshot.cellInputs["0,1"])
        assertEquals("0,2", savedSnapshot.focusedCellKey)

        val recreatedDependencies =
            buildFakeDependencies(
                context = appContext,
                prefsName = "rotate-resume",
            )
        val recreatedViewModel = newGameViewModel(recreatedDependencies)
        assertEquals("山", boardCellText(recreatedViewModel, row = 0, col = 1))

        recreatedViewModel.inputCandidate('流')
        assertEquals("流", boardCellText(recreatedViewModel, row = 0, col = 2))
    }

    private fun newGameViewModel(dependencies: WordDragonDependencies): GameViewModel =
        GameViewModel(
            levelId = "level-0001",
            idiomCatalogDataSource = dependencies.idiomCatalogDataSource,
            levelPackDataSource = dependencies.levelPackDataSource,
            progressStore = dependencies.progressStore,
            ttsSpeaker = dependencies.ttsSpeaker,
            errorSoundPlayer = dependencies.errorSoundPlayer,
        )

    private fun boardCellText(
        viewModel: GameViewModel,
        row: Int,
        col: Int,
    ): String =
        viewModel.uiState.value.boardCells.first { cell ->
            cell.row == row && cell.col == col
        }.text
}
