package me.lemonhall.worddragon.domain.game

import me.lemonhall.worddragon.domain.tts.GameSpeechFormatter

class GameSessionEngine(
    private val level: LevelDefinition,
    idioms: List<IdiomDefinition>,
) {
    private data class IdiomSlot(
        val idiom: IdiomDefinition,
        val orientation: WordOrientation,
        val coordinates: List<CellCoordinate>,
    )

    private data class BoardCellDefinition(
        val coordinate: CellCoordinate,
        val solutionChar: Char,
        val idiomIds: MutableSet<String>,
    )

    private data class SelectionTarget(
        val idiomId: String,
        val coordinate: CellCoordinate,
    )

    private val idiomsById = idioms.associateBy { it.id }
    private val idiomSlots: Map<String, IdiomSlot> = buildIdiomSlots()
    private val boardDefinitions: Map<CellCoordinate, BoardCellDefinition> = buildBoardDefinitions()
    private val candidatePoolTotals: LinkedHashMap<Char, Int> = buildCandidatePoolTotals()

    fun startSession(snapshot: LevelProgressSnapshot? = null): GameSessionState {
        val cellInputs =
            snapshot
                ?.takeIf { it.levelId == level.levelId }
                ?.cellInputs
                ?.mapNotNull { (key, value) ->
                    val coordinate = CellCoordinate.fromStorageKey(key) ?: return@mapNotNull null
                    if (!boardDefinitions.containsKey(coordinate) || value.isBlank()) {
                        return@mapNotNull null
                    }
                    val inputChar = value.first()
                    if (inputChar != boardDefinitions.getValue(coordinate).solutionChar) {
                        return@mapNotNull null
                    }
                    coordinate to inputChar
                }?.toMap()
                ?: emptyMap()
        val selectedIdiomId =
            resolveSelectedIdiomId(
                requestedIdiomId = snapshot?.selectedIdiomId,
                cellInputs = cellInputs,
            )
        val focusedCoordinate =
            resolveFocusedCoordinate(
                selectedIdiomId = selectedIdiomId,
                requestedFocus = snapshot?.focusedCellKey?.let(CellCoordinate::fromStorageKey),
                cellInputs = cellInputs,
            )
        return buildState(
            cellInputs = cellInputs,
            selectedIdiomId = selectedIdiomId,
            focusedCellCoordinate = focusedCoordinate,
            hintUsage = snapshot?.hintUsage ?: HintUsage(),
            currentSpeechText = idiomSlots.getValue(selectedIdiomId).idiom.speechText(),
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun selectIdiom(
        state: GameSessionState,
        idiomId: String,
    ): GameSessionState {
        if (!idiomSlots.containsKey(idiomId)) {
            return state
        }
        val focusedCoordinate =
            resolveFocusedCoordinate(
                selectedIdiomId = idiomId,
                requestedFocus = state.focusedCellCoordinate.takeIf { it in idiomSlots.getValue(idiomId).coordinates },
                cellInputs = state.cellInputs,
            )
        return buildState(
            cellInputs = state.cellInputs,
            selectedIdiomId = idiomId,
            focusedCellCoordinate = focusedCoordinate,
            hintUsage = state.hintUsage,
            currentSpeechText = idiomSlots.getValue(idiomId).idiom.speechText(),
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun selectCell(
        state: GameSessionState,
        coordinate: CellCoordinate,
    ): GameSessionState {
        val definition = boardDefinitions[coordinate] ?: return state
        val nextIdiomId =
            if (definition.idiomIds.size == 1) {
                definition.idiomIds.first()
            } else if (state.focusedCellCoordinate == coordinate && state.selectedIdiomId in definition.idiomIds) {
                definition.idiomIds.first { it != state.selectedIdiomId }
            } else {
                choosePreferredIdiomId(
                    currentState = state,
                    coordinate = coordinate,
                    idiomIds = definition.idiomIds.toList(),
                )
            }
        return buildState(
            cellInputs = state.cellInputs,
            selectedIdiomId = nextIdiomId,
            focusedCellCoordinate = coordinate,
            hintUsage = state.hintUsage,
            currentSpeechText = idiomSlots.getValue(nextIdiomId).idiom.speechText(),
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun inputCandidate(
        state: GameSessionState,
        candidate: Char,
    ): GameSessionState {
        if (state.isCompleted) {
            return state
        }
        val target = resolveInputTarget(state) ?: return state
        val currentValue = state.cellInputs[target.coordinate]
        if (currentValue == candidate) {
            return state
        }
        if (remainingCountFor(candidate, state.cellInputs) <= 0) {
            return state
        }
        if (candidate != boardDefinitions.getValue(target.coordinate).solutionChar) {
            return buildState(
                cellInputs = state.cellInputs,
                selectedIdiomId = state.selectedIdiomId,
                focusedCellCoordinate = state.focusedCellCoordinate,
                hintUsage = state.hintUsage,
                currentSpeechText = state.currentSpeechText,
                lastInputFeedback = InputFeedback.REJECTED,
            )
        }

        val nextInputs = state.cellInputs.toMutableMap()
        nextInputs[target.coordinate] = candidate
        val levelCompleted = isLevelComplete(nextInputs)
        val solvedCurrentIdiom = isIdiomSolved(target.idiomId, nextInputs)
        val nextSelection =
            when {
                levelCompleted -> target
                solvedCurrentIdiom -> firstPendingSelectionFor(nextUnfinishedIdiomId(nextInputs) ?: target.idiomId, nextInputs)
                else -> nextPendingSelectionInIdiom(target.idiomId, target.coordinate, nextInputs)
            }
        val speechText =
            when {
                levelCompleted -> LEVEL_COMPLETED_SPEECH
                !isIdiomSolved(target.idiomId, state.cellInputs) && solvedCurrentIdiom -> IDIOM_COMPLETED_SPEECH
                else -> state.currentSpeechText
            }
        return buildState(
            cellInputs = nextInputs,
            selectedIdiomId = nextSelection.idiomId,
            focusedCellCoordinate = nextSelection.coordinate,
            hintUsage = state.hintUsage,
            currentSpeechText = speechText,
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun revealSingleChar(state: GameSessionState): GameSessionState {
        if (state.isCompleted) {
            return state
        }
        val target = resolveInputTarget(state) ?: return state
        val slot = idiomSlots.getValue(target.idiomId)
        val revealIndex = slot.coordinates.indexOf(target.coordinate).takeIf { it >= 0 } ?: return state
        val nextInputs = state.cellInputs.toMutableMap()
        nextInputs[target.coordinate] = slot.idiom.text[revealIndex]
        val levelCompleted = isLevelComplete(nextInputs)
        val solvedCurrentIdiom = isIdiomSolved(target.idiomId, nextInputs)
        val nextSelection =
            when {
                levelCompleted -> target
                solvedCurrentIdiom -> firstPendingSelectionFor(nextUnfinishedIdiomId(nextInputs) ?: target.idiomId, nextInputs)
                else -> nextPendingSelectionInIdiom(target.idiomId, target.coordinate, nextInputs)
            }
        return buildState(
            cellInputs = nextInputs,
            selectedIdiomId = nextSelection.idiomId,
            focusedCellCoordinate = nextSelection.coordinate,
            hintUsage = state.hintUsage.copy(revealedChars = state.hintUsage.revealedChars + 1),
            currentSpeechText = if (levelCompleted) LEVEL_COMPLETED_SPEECH else HINT_CHAR_SPEECH,
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun revealWholeIdiom(state: GameSessionState): GameSessionState {
        if (state.isCompleted) {
            return state
        }
        val targetIdiomId =
            if (isIdiomSolved(state.selectedIdiomId, state.cellInputs)) {
                nextUnfinishedIdiomId(state.cellInputs) ?: state.selectedIdiomId
            } else {
                state.selectedIdiomId
            }
        val slot = idiomSlots.getValue(targetIdiomId)
        val nextInputs = state.cellInputs.toMutableMap()
        slot.coordinates.forEachIndexed { index, coordinate ->
            nextInputs[coordinate] = slot.idiom.text[index]
        }
        val levelCompleted = isLevelComplete(nextInputs)
        val nextSelection =
            when {
                levelCompleted -> SelectionTarget(targetIdiomId, slot.coordinates.first())
                else -> firstPendingSelectionFor(nextUnfinishedIdiomId(nextInputs) ?: targetIdiomId, nextInputs)
            }
        return buildState(
            cellInputs = nextInputs,
            selectedIdiomId = nextSelection.idiomId,
            focusedCellCoordinate = nextSelection.coordinate,
            hintUsage = state.hintUsage.copy(revealedIdioms = state.hintUsage.revealedIdioms + 1),
            currentSpeechText = if (levelCompleted) LEVEL_COMPLETED_SPEECH else HINT_IDIOM_SPEECH,
            lastInputFeedback = InputFeedback.NONE,
        )
    }

    fun snapshotOf(state: GameSessionState): LevelProgressSnapshot =
        LevelProgressSnapshot(
            levelId = level.levelId,
            selectedIdiomId = state.selectedIdiomId,
            focusedCellKey = state.focusedCellCoordinate.storageKey(),
            cellInputs =
                state.cellInputs.mapKeys { (coordinate, _) ->
                    coordinate.storageKey()
                }.mapValues { (_, value) ->
                    value.toString()
                },
            hintUsage = state.hintUsage,
            isCompleted = state.isCompleted,
        )

    private fun buildState(
        cellInputs: Map<CellCoordinate, Char>,
        selectedIdiomId: String,
        focusedCellCoordinate: CellCoordinate,
        hintUsage: HintUsage,
        currentSpeechText: String,
        lastInputFeedback: InputFeedback,
    ): GameSessionState {
        val idiomStates =
            level.idiomIds.map { idiomId ->
                val slot = idiomSlots.getValue(idiomId)
                val filledChars =
                    slot.coordinates.map { coordinate ->
                        cellInputs[coordinate] ?: EMPTY_SLOT_CHAR
                    }
                IdiomProgressState(
                    idiomId = idiomId,
                    solutionText = slot.idiom.text,
                    filledText = filledChars.joinToString(""),
                    shortExplanation = slot.idiom.shortExplanation,
                    isSolved = isIdiomSolved(idiomId, cellInputs),
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
                        isFocused = definition.coordinate == focusedCellCoordinate,
                        isCorrect = input == definition.solutionChar,
                    )
                }
        val candidateStates = buildCandidateStates(cellInputs)
        return GameSessionState(
            levelId = level.levelId,
            chapterId = level.chapterId,
            selectedIdiomId = selectedIdiomId,
            focusedCellCoordinate = focusedCellCoordinate,
            idiomStates = idiomStates,
            boardCells = boardCells,
            candidateChars = candidateStates.map { it.char },
            candidateStates = candidateStates,
            hintUsage = hintUsage,
            currentSpeechText = currentSpeechText,
            currentExplanation = idiomSlots.getValue(selectedIdiomId).idiom.shortExplanation,
            lastInputFeedback = lastInputFeedback,
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
            placement.idiomId to
                IdiomSlot(
                    idiom = idiom,
                    orientation = placement.orientation,
                    coordinates = coordinates,
                )
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

    private fun buildCandidatePoolTotals(): LinkedHashMap<Char, Int> {
        val totals = linkedMapOf<Char, Int>()
        level.candidateChars.forEach { candidate ->
            totals[candidate] = (totals[candidate] ?: 0) + 1
        }
        return LinkedHashMap(totals)
    }

    private fun buildCandidateStates(cellInputs: Map<CellCoordinate, Char>): List<CandidateCharState> =
        candidatePoolTotals.entries.map { (char, totalCount) ->
            val remaining = (totalCount - cellInputs.values.count { it == char }).coerceAtLeast(0)
            CandidateCharState(
                char = char,
                remainingCount = remaining,
                isEnabled = remaining > 0,
            )
        }

    private fun resolveSelectedIdiomId(
        requestedIdiomId: String?,
        cellInputs: Map<CellCoordinate, Char>,
    ): String {
        val validRequested = requestedIdiomId?.takeIf(idiomSlots::containsKey)
        if (validRequested != null && !isIdiomSolved(validRequested, cellInputs)) {
            return validRequested
        }
        return nextUnfinishedIdiomId(cellInputs) ?: validRequested ?: level.idiomIds.first()
    }

    private fun resolveFocusedCoordinate(
        selectedIdiomId: String,
        requestedFocus: CellCoordinate?,
        cellInputs: Map<CellCoordinate, Char>,
    ): CellCoordinate {
        val slot = idiomSlots.getValue(selectedIdiomId)
        return requestedFocus
            ?.takeIf { it in slot.coordinates }
            ?: firstPendingSelectionFor(selectedIdiomId, cellInputs).coordinate
    }

    private fun choosePreferredIdiomId(
        currentState: GameSessionState,
        coordinate: CellCoordinate,
        idiomIds: List<String>,
    ): String {
        val unfinished = idiomIds.filterNot { isIdiomSolved(it, currentState.cellInputs) }
        if (unfinished.size == 1) {
            return unfinished.first()
        }
        val candidates = unfinished.ifEmpty { idiomIds }
        val pendingFromCoordinate =
            candidates.filter { idiomId ->
                val slot = idiomSlots.getValue(idiomId)
                val startIndex = slot.coordinates.indexOf(coordinate)
                slot.coordinates.drop(startIndex).any { pendingCoordinate ->
                    currentState.cellInputs[pendingCoordinate] != boardDefinitions.getValue(pendingCoordinate).solutionChar
                }
            }
        if (pendingFromCoordinate.size == 1) {
            return pendingFromCoordinate.first()
        }
        if (currentState.selectedIdiomId in pendingFromCoordinate) {
            return currentState.selectedIdiomId
        }
        if (pendingFromCoordinate.isNotEmpty()) {
            return pendingFromCoordinate.first()
        }
        if (currentState.selectedIdiomId in candidates) {
            return currentState.selectedIdiomId
        }
        return candidates.firstOrNull { idiomSlots.getValue(it).orientation == WordOrientation.ACROSS } ?: candidates.first()
    }

    private fun resolveInputTarget(state: GameSessionState): SelectionTarget? {
        val currentSlot = idiomSlots.getValue(state.selectedIdiomId)
        val focus =
            state.focusedCellCoordinate.takeIf { it in currentSlot.coordinates }
                ?: firstPendingSelectionFor(state.selectedIdiomId, state.cellInputs).coordinate
        val currentValue = state.cellInputs[focus]
        val isCurrentCorrect = currentValue == boardDefinitions.getValue(focus).solutionChar
        if (currentValue == null || !isCurrentCorrect) {
            return SelectionTarget(state.selectedIdiomId, focus)
        }

        val nextInCurrent = nextPendingSelectionInIdiom(state.selectedIdiomId, focus, state.cellInputs)
        if (nextInCurrent.coordinate != focus || !isIdiomSolved(state.selectedIdiomId, state.cellInputs)) {
            return nextInCurrent
        }

        val nextIdiomId = nextUnfinishedIdiomId(state.cellInputs) ?: return null
        return firstPendingSelectionFor(nextIdiomId, state.cellInputs)
    }

    private fun nextPendingSelectionInIdiom(
        idiomId: String,
        fromCoordinate: CellCoordinate,
        cellInputs: Map<CellCoordinate, Char>,
    ): SelectionTarget {
        val slot = idiomSlots.getValue(idiomId)
        val startIndex = slot.coordinates.indexOf(fromCoordinate).coerceAtLeast(0)
        val pendingAfter =
            slot.coordinates.drop(startIndex + 1).firstOrNull { coordinate ->
                cellInputs[coordinate] != boardDefinitions.getValue(coordinate).solutionChar
            }
        if (pendingAfter != null) {
            return SelectionTarget(idiomId, pendingAfter)
        }
        return firstPendingSelectionFor(idiomId, cellInputs)
    }

    private fun firstPendingSelectionFor(
        idiomId: String,
        cellInputs: Map<CellCoordinate, Char>,
    ): SelectionTarget {
        val slot = idiomSlots.getValue(idiomId)
        val coordinate =
            slot.coordinates.firstOrNull { current ->
                cellInputs[current] != boardDefinitions.getValue(current).solutionChar
            } ?: slot.coordinates.first()
        return SelectionTarget(idiomId, coordinate)
    }

    private fun nextUnfinishedIdiomId(cellInputs: Map<CellCoordinate, Char>): String? =
        level.idiomIds.firstOrNull { idiomId ->
            !isIdiomSolved(idiomId, cellInputs)
        }

    private fun isIdiomSolved(
        idiomId: String,
        cellInputs: Map<CellCoordinate, Char>,
    ): Boolean {
        val slot = idiomSlots.getValue(idiomId)
        return slot.coordinates.indices.all { index ->
            cellInputs[slot.coordinates[index]] == slot.idiom.text[index]
        }
    }

    private fun isLevelComplete(cellInputs: Map<CellCoordinate, Char>): Boolean =
        level.idiomIds.all { idiomId ->
            isIdiomSolved(idiomId, cellInputs)
        }

    private fun remainingCountFor(
        candidate: Char,
        cellInputs: Map<CellCoordinate, Char>,
    ): Int {
        val total = candidatePoolTotals[candidate] ?: 0
        return (total - cellInputs.values.count { it == candidate }).coerceAtLeast(0)
    }

    private fun IdiomDefinition.speechText(): String = GameSpeechFormatter.format(this)

    private companion object {
        const val EMPTY_SLOT_CHAR = '□'
        const val IDIOM_COMPLETED_SPEECH = "已完成一条。"
        const val LEVEL_COMPLETED_SPEECH = "本关完成，可以进入下一关。"
        const val HINT_CHAR_SPEECH = "已提示一个字。"
        const val HINT_IDIOM_SPEECH = "已揭示当前成语。"
    }
}
