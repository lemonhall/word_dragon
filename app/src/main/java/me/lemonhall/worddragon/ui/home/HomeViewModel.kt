package me.lemonhall.worddragon.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.lemonhall.worddragon.data.content.LevelPackDataSource
import me.lemonhall.worddragon.data.progress.ProgressStore

data class HomeUiState(
    val continueLevelId: String? = null,
    val summaryText: String = "正在准备离线关卡……",
    val continueSubtitle: String = "从上次保存的位置继续；如果还没有进度，就从第一关开始。",
    val autoSpeakEnabled: Boolean = true,
)

class HomeViewModel(
    private val progressStore: ProgressStore,
    private val levelPackDataSource: LevelPackDataSource,
) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        val progress = progressStore.readProgress()
        val allLevelIds = levelPackDataSource.allLevelIds()
        val continueLevelId =
            progress.currentLevelId
                ?: allLevelIds.firstOrNull { it !in progress.completedLevelIds }
                ?: allLevelIds.lastOrNull()
        val completedCount = progress.completedLevelIds.size
        val summaryText =
            if (continueLevelId == null) {
                "当前没有可用关卡，请检查内置内容包。"
            } else if (completedCount == 0) {
                "还没有本地进度，建议从第一关慢慢开始。"
            } else {
                "已通关 $completedCount 关，继续从 $continueLevelId 往后玩。"
            }
        val continueSubtitle =
            if (continueLevelId != null && progress.snapshots.containsKey(continueLevelId)) {
                "上次的盘面已经自动保存，点进去就能接着填。"
            } else {
                "从当前已解锁的关卡继续；如果已经全部通关，也可以重玩最后一关。"
            }
        _uiState.value =
            HomeUiState(
                continueLevelId = continueLevelId,
                summaryText = summaryText,
                continueSubtitle = continueSubtitle,
                autoSpeakEnabled = progress.autoSpeakEnabled,
            )
    }
}
