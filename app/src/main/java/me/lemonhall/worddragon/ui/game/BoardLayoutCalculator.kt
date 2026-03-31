package me.lemonhall.worddragon.ui.game

import kotlin.math.floor

data class BoardLayoutMetrics(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val cellSpacingDp: Int,
    val cellSizeDp: Int,
    val requiresHorizontalScroll: Boolean,
) {
    fun requiredWidthDp(boardWidth: Int): Int =
        (horizontalPaddingDp * 2) + (cellSizeDp * boardWidth) + (cellSpacingDp * (boardWidth - 1).coerceAtLeast(0))
}

object BoardLayoutCalculator {
    private const val CompactPaddingDp = 12
    private const val ComfortablePaddingDp = 20
    private const val CompactSpacingDp = 4
    private const val ComfortableSpacingDp = 8
    private const val PreferredCellSizeDp = 56
    private const val MinimumReadableCellSizeDp = 34

    fun calculate(
        availableWidthDp: Int,
        boardWidth: Int,
    ): BoardLayoutMetrics {
        require(availableWidthDp > 0) { "availableWidthDp must be positive" }
        require(boardWidth > 0) { "boardWidth must be positive" }

        val useCompactLayout = boardWidth >= 6 || availableWidthDp <= 320
        val horizontalPaddingDp = if (useCompactLayout) CompactPaddingDp else ComfortablePaddingDp
        val verticalPaddingDp = if (useCompactLayout) CompactPaddingDp else ComfortablePaddingDp
        val cellSpacingDp = if (useCompactLayout) CompactSpacingDp else ComfortableSpacingDp
        val availableBoardWidthDp =
            availableWidthDp - (horizontalPaddingDp * 2) - (cellSpacingDp * (boardWidth - 1).coerceAtLeast(0))
        val fittedCellSizeDp = floor(availableBoardWidthDp.toDouble() / boardWidth).toInt()
        val cellSizeDp = fittedCellSizeDp.coerceIn(MinimumReadableCellSizeDp, PreferredCellSizeDp)
        val requiredWidthDp =
            (horizontalPaddingDp * 2) + (cellSizeDp * boardWidth) + (cellSpacingDp * (boardWidth - 1).coerceAtLeast(0))

        return BoardLayoutMetrics(
            horizontalPaddingDp = horizontalPaddingDp,
            verticalPaddingDp = verticalPaddingDp,
            cellSpacingDp = cellSpacingDp,
            cellSizeDp = cellSizeDp,
            requiresHorizontalScroll = requiredWidthDp > availableWidthDp,
        )
    }
}
