package me.lemonhall.worddragon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import me.lemonhall.worddragon.ui.chapters.ChapterListScreen
import me.lemonhall.worddragon.ui.home.HomeScreen
import me.lemonhall.worddragon.ui.settings.SettingsScreen

private enum class WordDragonDestination {
    HOME,
    CHAPTERS,
    SETTINGS,
}

@Composable
fun WordDragonNavGraph() {
    var destinationName by rememberSaveable { mutableStateOf(WordDragonDestination.HOME.name) }
    var chapterBanner by rememberSaveable { mutableStateOf("请选择一个章节，慢慢开始。") }

    when (WordDragonDestination.valueOf(destinationName)) {
        WordDragonDestination.HOME ->
            HomeScreen(
                onContinueGame = {
                    chapterBanner = "当前还没有本地进度，将从第一章开始。"
                    destinationName = WordDragonDestination.CHAPTERS.name
                },
                onOpenChapters = {
                    chapterBanner = "请选择想挑战的章节。"
                    destinationName = WordDragonDestination.CHAPTERS.name
                },
                onOpenSettings = {
                    destinationName = WordDragonDestination.SETTINGS.name
                },
            )

        WordDragonDestination.CHAPTERS ->
            ChapterListScreen(
                banner = chapterBanner,
                onBack = { destinationName = WordDragonDestination.HOME.name },
            )

        WordDragonDestination.SETTINGS ->
            SettingsScreen(
                onBack = { destinationName = WordDragonDestination.HOME.name },
            )
    }
}
