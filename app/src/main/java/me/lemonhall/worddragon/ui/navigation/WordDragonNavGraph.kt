package me.lemonhall.worddragon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import me.lemonhall.worddragon.WordDragonDependencies
import me.lemonhall.worddragon.ui.chapters.ChapterListScreen
import me.lemonhall.worddragon.ui.chapters.ChapterListViewModel
import me.lemonhall.worddragon.ui.game.GameScreen
import me.lemonhall.worddragon.ui.game.GameViewModel
import me.lemonhall.worddragon.ui.home.HomeScreen
import me.lemonhall.worddragon.ui.home.HomeViewModel
import me.lemonhall.worddragon.ui.settings.SettingsScreen

private enum class WordDragonDestination {
    HOME,
    CHAPTERS,
    SETTINGS,
    GAME,
}

@Composable
fun WordDragonNavGraph(dependencies: WordDragonDependencies) {
    val homeViewModel =
        remember(dependencies) {
            HomeViewModel(
                progressStore = dependencies.progressStore,
                levelPackDataSource = dependencies.levelPackDataSource,
            )
        }
    val chapterListViewModel =
        remember(dependencies) {
            ChapterListViewModel(
                progressStore = dependencies.progressStore,
                levelPackDataSource = dependencies.levelPackDataSource,
            )
        }
    val homeUiState by homeViewModel.uiState.collectAsState()
    val chapterUiState by chapterListViewModel.uiState.collectAsState()

    var destinationName by rememberSaveable { mutableStateOf(WordDragonDestination.HOME.name) }
    var activeLevelId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(destinationName, activeLevelId) {
        homeViewModel.refresh()
        chapterListViewModel.refresh()
    }

    when (WordDragonDestination.valueOf(destinationName)) {
        WordDragonDestination.HOME ->
            HomeScreen(
                summaryText = homeUiState.summaryText,
                continueSubtitle = homeUiState.continueSubtitle,
                onContinueGame = {
                    activeLevelId = homeUiState.continueLevelId
                    destinationName = WordDragonDestination.GAME.name
                },
                onOpenChapters = {
                    destinationName = WordDragonDestination.CHAPTERS.name
                },
                onOpenSettings = {
                    destinationName = WordDragonDestination.SETTINGS.name
                },
            )

        WordDragonDestination.CHAPTERS ->
            ChapterListScreen(
                uiState = chapterUiState,
                onOpenLevel = { levelId ->
                    activeLevelId = levelId
                    destinationName = WordDragonDestination.GAME.name
                },
                onBack = { destinationName = WordDragonDestination.HOME.name },
            )

        WordDragonDestination.SETTINGS ->
            SettingsScreen(
                autoSpeakEnabled = homeUiState.autoSpeakEnabled,
                onAutoSpeakChanged = {
                    dependencies.progressStore.setAutoSpeakEnabled(it)
                    homeViewModel.refresh()
                },
                onBack = { destinationName = WordDragonDestination.HOME.name },
            )

        WordDragonDestination.GAME -> {
            val levelId = activeLevelId ?: homeUiState.continueLevelId ?: dependencies.levelPackDataSource.firstLevelId()
            if (levelId == null) {
                destinationName = WordDragonDestination.HOME.name
            } else {
                val gameViewModel =
                    remember(dependencies, levelId) {
                        GameViewModel(
                            levelId = levelId,
                            idiomCatalogDataSource = dependencies.idiomCatalogDataSource,
                            levelPackDataSource = dependencies.levelPackDataSource,
                            progressStore = dependencies.progressStore,
                            ttsSpeaker = dependencies.ttsSpeaker,
                        )
                    }
                val gameUiState by gameViewModel.uiState.collectAsState()

                GameScreen(
                    uiState = gameUiState,
                    onBack = {
                        homeViewModel.refresh()
                        chapterListViewModel.refresh()
                        destinationName = WordDragonDestination.HOME.name
                    },
                    onSelectIdiom = gameViewModel::selectIdiom,
                    onInputCandidate = gameViewModel::inputCandidate,
                    onRevealSingleChar = gameViewModel::revealSingleChar,
                    onRevealWholeIdiom = gameViewModel::revealWholeIdiom,
                    onReplaySpeech = gameViewModel::replaySelectedIdiom,
                    onToggleAutoSpeak = gameViewModel::toggleAutoSpeak,
                    onOpenNextLevel = { nextLevelId ->
                        activeLevelId = nextLevelId
                    },
                )
            }
        }
    }
}
