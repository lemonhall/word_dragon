package me.lemonhall.worddragon.domain.game

import me.lemonhall.worddragon.domain.tts.GameSpeechFormatter

class GameSessionEngine(
    private val level: LevelDefinition,
    idioms: List<IdiomDefinition>,
) {
    private data class IdiomSlot(
        val idiom: IdiomDefinition,
        val coordinates: List<CellCoordinate>,
    )

    private data class BoardCellDefinition(
        val coordinate: CellCoordinate,
        val solutionChar: Char,
        val idiomIds: MutableSet<String>,
    )

    private val idiomsById = idioms.associateBy { it.id }
    private val idiomSlots: Map<String, IdiomSlot> = buildIdiomSlots()
    private val boardDefinitions: Map<CellCoordinate, BoardCellDefinition> = buildBoardDefinitions()

    fun startSession(snapshot: LevelProgressSnapshot? = null): GameSessionState {
        val selectedIdiomId =
            snapshot
                ?.selectedIdiomId
                ?.takeIf(idiomSlots::containsKey)
                ?: level.idiomIds.first()
        val cellInputs =
            snapshot
                ?.takeIf { it.levelId == level.levelId }
                ?.cellInputs
                ?.mapNotNull { (key, value) ->
                    val coordinate = CellCoordinate.fromStorageKey(key) ?: return@mapNotNull null
                    if (!boardDefinitions.containsKey(coordinate) || value.isBlank()) {
                        return@mapNotNull null
                    }
                    coordinate to value.first()
                }?.toMap()
                ?: emptyMap()
        val speechText = idiomSlots.getValue(level.idiomIds.first()).idiom.speechText()
        return buildState(
            cellInputs = cellInputs,
            selectedIdiomId = selectedIdiomId,
            hintUsage = snapshot?.hintUsage ?: HintUsage(),
            currentSpeechText = speechText,
        )
    }

    fun selectIdiom(
        state: GameSessionState,
        idiomId: String,
    ): GameSessionState {
        if (!idiomSlots.containsKey(idiomId)) {
            return state
        }
        return buildState(
            cellInputs = state.cellInputs,
            selectedIdiomId = idiomId,
            hintUsage = state.hintUsage,
            currentSpeechText = state.currentSpeechText,
        )
    }

    fun inputCandidate(
        state: GameSessionState,
        candidate: Char,
    ): GameSessionState {
        val slot = idiomSlots.getValue(state.selectedIdiomId)
        val targetIndex =
            slot.coordinates.indexOfFirst { coordinate ->
                state.cellInputs[coordinate] == null
            }.takeIf { it >= 0 }
                ?: slot.coordinates.indices.indexOfFirst { index ->
                    state.cellInputs[slot.coordinates[index]] != slot.idiom.text[index]
                }.takeIf { it >= 0 }
                ?: return state

        val nextInputs = state.cellInputs.toMutableMap()
        nextInputs[slot.coordinates[targetIndex]] = candidate

        val nextState =
            buildState(
                cellInputs = nextInputs,
                selectedIdiomId = state.selectedIdiomId,
                hintUsage = state.hintUsage,
                currentSpeechText = nextSpeechTextAfterSelectionResolve(state, slot, nextInputs),
            )
        return nextState
    }

    fun revealSingleChar(state: GameSessionState): GameSessionState {
        val slot = idiomSlots.getValue(state.selectedIdiomId)
        val reveal =
            HintPolicy.revealSingleChar(
                solution = slot.idiom.text,
                current = currentIdiomText(slot, state.cellInputs),
            ) ?: return state
        val nextInputs = state.cellInputs.toMutableMap()
        nextInputs[slot.coordinates[reveal.index]] = reveal.char
        return buildState(
            cellInputs = nextInputs,
            selectedIdiomId = state.selectedIdiomId,
            hintUsage = state.hintUsage.copy(revealedChars = state.hintUsage.revealedChars + 1),
            currentSpeechText = nextSpeechTextAfterSelectionResolve(state, slot, nextInputs),
        )
    }

    fun revealWholeIdiom(state: GameSessionState): GameSessionState {
        val slot = idiomSlots.getValue(state.selectedIdiomId)
        val nextInputs = state.cellInputs.toMutableMap()
        slot.coordinates.forEachIndexed { index, coordinate ->
            nextInputs[coordinate] = slot.idiom.text[index]
        }
        return buildState(
            cellInputs = nextInputs,
            selectedIdiomId = state.selectedIdiomId,
            hintUsage = state.hintUsage.copy(revealedIdioms = state.hintUsage.revealedIdioms + 1),
            currentSpeechText = nextSpeechTextAfterSelectionResolve(state, slot, nextInputs),
        )
    }

    fun snapshotOf(state: GameSessionState): LevelProgressSnapshot =
        LevelProgressSnapshot(
            levelId = level.levelId,
            selectedIdiomId = state.selectedIdiomId,
            cellInputs =
                state.cellInputs.mapKeys { (coordinate, _) ->
                    coordinate.storageKey()
                }.mapValues { (_, value) ->
                    value.toString()
                },
            hintUsage = state.hintUsage,
        )

    private fun buildState(
        cellInputs: Map<CellCoordinate, Char>,
        selectedIdiomId: String,
        hintUsage: HintUsage,
        currentSpeechText: String,
    ): GameSessionState {
        val idiomStates =
            level.idiomIds.map { idiomId ->
                val slot = idiomSlots.getValue(idiomId)
                val filledChars =
                    slot.coordinates.mapIndexed { index, coordinate ->
                        cellInputs[coordinate] ?: EMPTY_SLOT_CHAR
                    }
                IdiomProgressState(
                    idiomId = idiomId,
                    solutionText = slot.idiom.text,
                    filledText = filledChars.joinToString(""),
                    shortExplanation = slot.idiom.shortExplanation,
                    isSolved = slot.coordinates.indices.all { index -> cellInputs[slot.coordinates[index]] == slot.idiom.text[index] },
                    coordinates = slot.coordinates,
                )
            }
        val selectedCoordinates = idiomSlots.getValue(selectedIdiomId).coordinates.toSet()
        val boardCells =
            boardDefinitions.values
                .sortedWith(compareBy<BoardCellDefinition>({ it.coordinate.row }, { it.coordinate.col }))
                .map { definition ->
                    val input = cellInputs[definition.coordinate]
                    BoardCellState(
                        coordinate = definition.coordinate,
                        solutionChar = definition.solutionChar,
                        inputChar = input,
                        idiomIds = definition.idiomIds.toSet(),
                        isSelected = definition.coordinate in selectedCoordinates,
                        isCorrect = input == definition.solutionChar,
                    )
                }
        val selectedIdiomState = idiomStates.first { it.idiomId == selectedIdiomId }
        return GameSessionState(
            levelId = level.levelId,
            chapterId = level.chapterId,
            selectedIdiomId = selectedIdiomId,
            idiomStates = idiomStates,
            boardCells = boardCells,
            candidateChars = level.candidateChars,
            hintUsage = hintUsage,
            currentSpeechText = currentSpeechText,
            currentExplanation = selectedIdiomState.shortExplanation,
            isCompleted = idiomStates.all { it.isSolved },
            cellInputs = cellInputs,
        )
    }

    private fun buildIdiomSlots(): Map<String, IdiomSlot> =
        level.placements.associate { placement ->
            val idiom = requireNotNull(idiomsById[placement.idiomId]) { "Missing idiom ${placement.idiomId}" }
            val coordinates =
                idiom.text.indices.map { index ->
                    when (placement.orientation) {
                        WordOrientation.ACROSS -> CellCoordinate(row = placement.row, col = placement.col + index)
                        WordOrientation.DOWN -> CellCoordinate(row = placement.row + index, col = placement.col)
                    }
                }
            placement.idiomId to IdiomSlot(idiom = idiom, coordinates = coordinates)
        }

    private fun buildBoardDefinitions(): Map<CellCoordinate, BoardCellDefinition> {
        val cells = linkedMapOf<CellCoordinate, BoardCellDefinition>()
        idiomSlots.values.forEach { slot ->
            slot.coordinates.forEachIndexed { index, coordinate ->
                val definition = cells[coordinate]
                if (definition == null) {
                    cells[coordinate] =
                        BoardCellDefinition(
                            coordinate = coordinate,
                            solutionChar = slot.idiom.text[index],
                            idiomIds = mutableSetOf(slot.idiom.id),
                        )
                } else {
                    require(definition.solutionChar == slot.idiom.text[index]) {
                        "Conflict at ${coordinate.storageKey()}: ${definition.solutionChar} vs ${slot.idiom.text[index]}"
                    }
                    definition.idiomIds += slot.idiom.id
                }
            }
        }
        return cells
    }

    private fun currentIdiomText(
        slot: IdiomSlot,
        inputs: Map<CellCoordinate, Char>,
    ): String =
        slot.coordinates.joinToString("") { coordinate ->
            inputs[coordinate]?.toString().orEmpty()
        }

    private fun nextSpeechTextAfterSelectionResolve(
        previousState: GameSessionState,
        slot: IdiomSlot,
        nextInputs: Map<CellCoordinate, Char>,
    ): String {
        val wasSolved = slot.coordinates.indices.all { index -> previousState.cellInputs[slot.coordinates[index]] == slot.idiom.text[index] }
        val isNowSolved = slot.coordinates.indices.all { index -> nextInputs[slot.coordinates[index]] == slot.idiom.text[index] }
        val isLevelComplete =
            level.idiomIds.all { idiomId ->
                val idiomSlot = idiomSlots.getValue(idiomId)
                idiomSlot.coordinates.indices.all { index -> nextInputs[idiomSlot.coordinates[index]] == idiomSlot.idiom.text[index] }
            }
        return if (!wasSolved && isNowSolved && !isLevelComplete) {
            slot.idiom.speechText()
        } else {
            previousState.currentSpeechText
        }
    }

    private fun IdiomDefinition.speechText(): String = GameSpeechFormatter.format(this)

    private companion object {
        const val EMPTY_SLOT_CHAR = '□'
    }
}
