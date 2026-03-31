package me.lemonhall.worddragon.domain.game

data class CellCoordinate(
    val row: Int,
    val col: Int,
) {
    fun storageKey(): String = "$row,$col"

    companion object {
        fun fromStorageKey(value: String): CellCoordinate? {
            val parts = value.split(",")
            if (parts.size != 2) {
                return null
            }
            val row = parts[0].toIntOrNull() ?: return null
            val col = parts[1].toIntOrNull() ?: return null
            return CellCoordinate(row = row, col = col)
        }
    }
}

enum class WordOrientation {
    ACROSS,
    DOWN,
}

data class LevelPlacement(
    val idiomId: String,
    val orientation: WordOrientation,
    val row: Int,
    val col: Int,
)

data class LevelDefinition(
    val levelId: String,
    val chapterId: String,
    val idiomIds: List<String>,
    val boardWidth: Int,
    val boardHeight: Int,
    val candidateChars: List<Char>,
    val placements: List<LevelPlacement>,
    val layoutProfile: String = "",
)

data class IdiomDefinition(
    val id: String,
    val text: String,
    val shortExplanation: String,
    val ttsText: String,
    val pinyin: String = "",
)

data class HintUsage(
    val revealedChars: Int = 0,
    val revealedIdioms: Int = 0,
)

data class LevelProgressSnapshot(
    val levelId: String,
    val selectedIdiomId: String,
    val focusedCellKey: String? = null,
    val cellInputs: Map<String, String>,
    val hintUsage: HintUsage = HintUsage(),
    val isCompleted: Boolean = false,
)

data class IdiomProgressState(
    val idiomId: String,
    val solutionText: String,
    val filledText: String,
    val shortExplanation: String,
    val isSolved: Boolean,
    val coordinates: List<CellCoordinate>,
)

data class BoardCellState(
    val coordinate: CellCoordinate,
    val solutionChar: Char,
    val inputChar: Char?,
    val idiomIds: Set<String>,
    val isSelected: Boolean,
    val isFocused: Boolean = false,
    val isCorrect: Boolean,
)

data class CandidateCharState(
    val char: Char,
    val remainingCount: Int,
    val isEnabled: Boolean,
)

enum class InputFeedback {
    NONE,
    REJECTED,
}

data class GameSessionState(
    val levelId: String,
    val chapterId: String,
    val selectedIdiomId: String,
    val focusedCellCoordinate: CellCoordinate,
    val idiomStates: List<IdiomProgressState>,
    val boardCells: List<BoardCellState>,
    val candidateChars: List<Char>,
    val candidateStates: List<CandidateCharState> = emptyList(),
    val hintUsage: HintUsage,
    val currentSpeechText: String,
    val currentExplanation: String,
    val lastInputFeedback: InputFeedback = InputFeedback.NONE,
    val isCompleted: Boolean,
    val cellInputs: Map<CellCoordinate, Char> = emptyMap(),
)
