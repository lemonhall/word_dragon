package me.lemonhall.worddragon.ui.game

import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.lemonhall.worddragon.audio.ErrorSoundPlayer
import me.lemonhall.worddragon.data.content.IdiomCatalogDataSource
import me.lemonhall.worddragon.data.content.LevelPackDataSource
import me.lemonhall.worddragon.domain.game.CellCoordinate
import me.lemonhall.worddragon.data.progress.ProgressStore
import me.lemonhall.worddragon.domain.game.BoardCellState
import me.lemonhall.worddragon.domain.game.CandidateCharState
import me.lemonhall.worddragon.domain.game.GameSessionEngine
import me.lemonhall.worddragon.domain.game.GameSessionState
import me.lemonhall.worddragon.domain.game.IdiomDefinition
import me.lemonhall.worddragon.domain.game.InputFeedback
import me.lemonhall.worddragon.domain.game.LevelDefinition
import me.lemonhall.worddragon.domain.tts.GameSpeechFormatter
import me.lemonhall.worddragon.tts.SpeakResult
import me.lemonhall.worddragon.tts.TtsSpeaker

data class GameBoardCellUiState(
    val row: Int,
    val col: Int,
    val text: String,
    val isSelected: Boolean,
    val isFocused: Boolean,
    val isCorrect: Boolean,
)

data class GameCandidateUiState(
    val char: Char,
    val remainingCount: Int,
    val isEnabled: Boolean,
)

data class GameUiState(
    val levelId: String = "",
    val chapterId: String = "",
    val levelTitle: String = "",
    val boardWidth: Int = 0,
    val boardHeight: Int = 0,
    val boardCells: List<GameBoardCellUiState> = emptyList(),
    val candidateStates: List<GameCandidateUiState> = emptyList(),
    val selectedIdiomId: String = "",
    val currentExplanation: String = "",
    val autoSpeakEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val nextLevelId: String? = null,
    val noticeText: String? = null,
)

class GameViewModel(
    private val levelId: String,
    private val idiomCatalogDataSource: IdiomCatalogDataSource,
    private val levelPackDataSource: LevelPackDataSource,
    private val progressStore: ProgressStore,
    private val ttsSpeaker: TtsSpeaker,
    private val errorSoundPlayer: ErrorSoundPlayer,
) {
    private val _uiState = MutableStateFlow(GameUiState(levelId = levelId))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private lateinit var levelDefinition: LevelDefinition
    private lateinit var engine: GameSessionEngine
    private lateinit var idiomsById: Map<String, IdiomDefinition>
    private var sessionState: GameSessionState? = null

    init {
        loadLevel()
    }

    fun selectCell(
        row: Int,
        col: Int,
    ) {
        updateSession(
            transform = { state ->
                engine.selectCell(
                    state = state,
                    coordinate = CellCoordinate(row = row, col = col),
                )
            },
            persistSnapshot = true,
            autoSpeakSelectedIdiom = true,
        )
    }

    fun inputCandidate(candidate: Char) {
        updateSession(
            transform = { state -> engine.inputCandidate(state, candidate) },
            persistSnapshot = true,
            autoSpeakSelectedIdiom = false,
        )
    }

    fun revealSingleChar() {
        updateSession(
            transform = engine::revealSingleChar,
            persistSnapshot = true,
            autoSpeakSelectedIdiom = false,
        )
    }

    fun revealWholeIdiom() {
        updateSession(
            transform = engine::revealWholeIdiom,
            persistSnapshot = true,
            autoSpeakSelectedIdiom = false,
        )
    }

    fun replaySelectedIdiom() {
        sessionState?.let { state ->
            speakIdiom(state.selectedIdiomId, allowAutoSpeakDisabled = true)
        }
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        progressStore.setAutoSpeakEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoSpeakEnabled = enabled)
    }

    private fun loadLevel() {
        levelDefinition = levelPackDataSource.readLevel(levelId)
        val idioms = idiomCatalogDataSource.getIdioms(levelDefinition.idiomIds)
        idiomsById = idioms.associateBy { it.id }
        engine = GameSessionEngine(levelDefinition, idioms)
        val snapshot = progressStore.readProgress().snapshots[levelId]
        sessionState = engine.startSession(snapshot)
        publishState(noticeText = null)
        if (_uiState.value.autoSpeakEnabled) {
            speakIdiom(sessionState!!.selectedIdiomId, allowAutoSpeakDisabled = false)
        }
    }

    private fun updateSession(
        transform: (GameSessionState) -> GameSessionState,
        persistSnapshot: Boolean,
        autoSpeakSelectedIdiom: Boolean,
    ) {
        val currentSession = sessionState ?: return
        val nextSession = transform(currentSession)
        val newlyCompletedIdiomIds = newlyCompletedIdiomIds(currentSession, nextSession)
        val becameCompleted = !currentSession.isCompleted && nextSession.isCompleted
        val shouldSpeakAutoAdvancedIdiom =
            shouldSpeakAutoAdvancedIdiom(
                currentSession = currentSession,
                nextSession = nextSession,
                autoSpeakSelectedIdiom = autoSpeakSelectedIdiom,
            )
        sessionState = nextSession

        if (persistSnapshot && !nextSession.isCompleted && snapshotRelevantStateChanged(currentSession, nextSession)) {
            progressStore.saveSnapshot(engine.snapshotOf(nextSession))
        }
        if (becameCompleted) {
            progressStore.markLevelCompleted(levelId = levelId, nextLevelId = levelPackDataSource.nextLevelId(levelId))
        }

        publishState(
            noticeText =
                if (becameCompleted) {
                    "通关成功，下一关已经解锁。"
                } else {
                    _uiState.value.noticeText
                },
        )

        if (nextSession.lastInputFeedback == InputFeedback.REJECTED) {
            errorSoundPlayer.playReject()
        }
        if (_uiState.value.autoSpeakEnabled) {
            speakCompletedIdioms(newlyCompletedIdiomIds)
        }
        if ((autoSpeakSelectedIdiom || shouldSpeakAutoAdvancedIdiom) && _uiState.value.autoSpeakEnabled) {
            speakIdiom(nextSession.selectedIdiomId, allowAutoSpeakDisabled = false)
        }
    }

    private fun publishState(noticeText: String?) {
        val currentSession = sessionState ?: return
        val progress = progressStore.readProgress()
        _uiState.value =
            GameUiState(
                levelId = currentSession.levelId,
                chapterId = currentSession.chapterId,
                levelTitle = formatLevelTitle(currentSession.levelId),
                boardWidth = levelDefinition.boardWidth,
                boardHeight = levelDefinition.boardHeight,
                boardCells = currentSession.boardCells.map { boardCell -> boardCell.toUiState() },
                candidateStates = currentSession.candidateStates.map { candidateState -> candidateState.toUiState() },
                selectedIdiomId = currentSession.selectedIdiomId,
                currentExplanation = currentSession.currentExplanation,
                autoSpeakEnabled = progress.autoSpeakEnabled,
                isCompleted = currentSession.isCompleted,
                nextLevelId = levelPackDataSource.nextLevelId(levelId),
                noticeText = noticeText,
            )
    }

    private fun speakIdiom(
        idiomId: String,
        allowAutoSpeakDisabled: Boolean,
    ) {
        val currentUiState = _uiState.value
        if (!allowAutoSpeakDisabled && !currentUiState.autoSpeakEnabled) {
            return
        }
        val idiom = idiomsById.getValue(idiomId)
        when (val result = ttsSpeaker.speak(GameSpeechFormatter.format(idiom))) {
            SpeakResult.Accepted -> {
                if (currentUiState.noticeText == "语音暂不可用。") {
                    _uiState.value = currentUiState.copy(noticeText = null)
                }
            }

            is SpeakResult.Unavailable -> {
                _uiState.value = currentUiState.copy(noticeText = result.reason)
            }
        }
    }

    private fun speakCompletedIdioms(idiomIds: List<String>) {
        idiomIds.forEach { idiomId ->
            val idiom = idiomsById.getValue(idiomId)
            ttsSpeaker.speak(GameSpeechFormatter.formatCompletedIdiom(idiom))
        }
    }

    private fun newlyCompletedIdiomIds(
        currentSession: GameSessionState,
        nextSession: GameSessionState,
    ): List<String> {
        val previousSolvedById = currentSession.idiomStates.associateBy({ it.idiomId }, { it.isSolved })
        return nextSession.idiomStates
            .filter { idiomState -> !previousSolvedById.getOrDefault(idiomState.idiomId, false) && idiomState.isSolved }
            .map { idiomState -> idiomState.idiomId }
    }

    private fun shouldSpeakAutoAdvancedIdiom(
        currentSession: GameSessionState,
        nextSession: GameSessionState,
        autoSpeakSelectedIdiom: Boolean,
    ): Boolean {
        if (autoSpeakSelectedIdiom || nextSession.isCompleted || currentSession.selectedIdiomId == nextSession.selectedIdiomId) {
            return false
        }
        return nextSession.idiomStates.any { idiomState ->
            idiomState.idiomId == nextSession.selectedIdiomId && !idiomState.isSolved
        }
    }

    private fun formatLevelTitle(levelId: String): String =
        levelId.removePrefix("level-").toIntOrNull()?.let { sequence ->
            String.format(Locale.ROOT, "第%04d级", sequence)
        } ?: levelId

    private fun BoardCellState.toUiState(): GameBoardCellUiState =
        GameBoardCellUiState(
            row = coordinate.row,
            col = coordinate.col,
            text = inputChar?.toString().orEmpty(),
            isSelected = isSelected,
            isFocused = isFocused,
            isCorrect = isCorrect,
        )

    private fun CandidateCharState.toUiState(): GameCandidateUiState =
        GameCandidateUiState(
            char = char,
            remainingCount = remainingCount,
            isEnabled = isEnabled,
        )

    private fun snapshotRelevantStateChanged(
        currentSession: GameSessionState,
        nextSession: GameSessionState,
    ): Boolean =
        currentSession.cellInputs != nextSession.cellInputs ||
            currentSession.selectedIdiomId != nextSession.selectedIdiomId ||
            currentSession.focusedCellCoordinate != nextSession.focusedCellCoordinate ||
            currentSession.hintUsage != nextSession.hintUsage ||
            currentSession.isCompleted != nextSession.isCompleted
}
