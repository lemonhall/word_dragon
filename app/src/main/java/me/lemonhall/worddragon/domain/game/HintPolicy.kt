package me.lemonhall.worddragon.domain.game

object HintPolicy {
    data class SingleCharReveal(
        val index: Int,
        val char: Char,
    )

    data class WholeIdiomReveal(
        val text: String,
        val changedCellCount: Int,
    )

    fun revealSingleChar(
        solution: String,
        current: String,
    ): SingleCharReveal? {
        val normalizedCurrent = current.padEnd(solution.length, ' ')
        val revealIndex =
            solution.indices.firstOrNull { index ->
                normalizedCurrent[index] != solution[index]
            } ?: return null
        return SingleCharReveal(index = revealIndex, char = solution[revealIndex])
    }

    fun revealWholeIdiom(
        solution: String,
        current: String,
    ): WholeIdiomReveal {
        val normalizedCurrent = current.padEnd(solution.length, ' ')
        val changedCellCount =
            solution.indices.count { index ->
                normalizedCurrent[index] != solution[index]
            }
        return WholeIdiomReveal(text = solution, changedCellCount = changedCellCount)
    }
}
