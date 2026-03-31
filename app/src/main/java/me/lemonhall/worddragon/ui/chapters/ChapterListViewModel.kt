package me.lemonhall.worddragon.ui.chapters

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.lemonhall.worddragon.data.content.LevelPackDataSource
import me.lemonhall.worddragon.data.progress.ProgressStore

data class LevelEntryUiState(
    val levelId: String,
    val label: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
)

data class ChapterEntryUiState(
    val chapterId: String,
    val title: String,
    val summary: String,
    val levelEntries: List<LevelEntryUiState>,
)

data class ChapterListUiState(
    val banner: String = "章节正在加载……",
    val chapters: List<ChapterEntryUiState> = emptyList(),
)

class ChapterListViewModel(
    private val progressStore: ProgressStore,
    private val levelPackDataSource: LevelPackDataSource,
) {
    private val _uiState = MutableStateFlow(ChapterListUiState())
    val uiState: StateFlow<ChapterListUiState> = _uiState.asStateFlow()

    fun refresh() {
        val progress = progressStore.readProgress()
        val allLevelIds = levelPackDataSource.allLevelIds()
        val currentLevelId =
            progress.currentLevelId
                ?: allLevelIds.firstOrNull { it !in progress.completedLevelIds }
        val unlockedBoundaryIndex =
            when {
                allLevelIds.isEmpty() -> -1
                currentLevelId == null -> allLevelIds.lastIndex
                else -> allLevelIds.indexOf(currentLevelId).coerceAtLeast(0)
            }
        val chapters =
            levelPackDataSource.readChapters().map { chapter ->
                val levelEntries =
                    chapter.levelIds.mapIndexed { index, levelId ->
                        LevelEntryUiState(
                            levelId = levelId,
                            label = (index + 1).toString(),
                            isUnlocked =
                                progress.completedLevelIds.contains(levelId) ||
                                    allLevelIds.indexOf(levelId) <= unlockedBoundaryIndex,
                            isCompleted = progress.completedLevelIds.contains(levelId),
                            isCurrent = currentLevelId == levelId,
                        )
                    }
                ChapterEntryUiState(
                    chapterId = chapter.chapterId,
                    title = chapter.title,
                    summary = "共 ${chapter.levelCount} 关，已完成 ${levelEntries.count { it.isCompleted }} 关。",
                    levelEntries = levelEntries,
                )
            }
        val banner =
            if (currentLevelId == null) {
                "当前没有未完成关卡，可以自由重玩。"
            } else {
                "当前继续关卡：$currentLevelId。已解锁的关卡都可以直接进入。"
            }
        _uiState.value = ChapterListUiState(banner = banner, chapters = chapters)
    }
}
