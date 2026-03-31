package me.lemonhall.worddragon.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.lemonhall.worddragon.ui.theme.WordDragonDimensions

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun GameScreen(
    uiState: GameUiState,
    onBack: () -> Unit,
    onSelectCell: (Int, Int) -> Unit,
    onInputCandidate: (Char) -> Unit,
    onRevealSingleChar: () -> Unit,
    onRevealWholeIdiom: () -> Unit,
    onReplaySpeech: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onOpenNextLevel: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "关卡练习",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "返回", style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("game-screen"),
        ) {
            val isWide = maxWidth >= 840.dp
            if (isWide) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(WordDragonDimensions.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(WordDragonDimensions.SectionSpacing),
                ) {
                    GameBoardCard(
                        uiState = uiState,
                        modifier = Modifier.weight(1.1f),
                        onSelectCell = onSelectCell,
                    )
                    GameControlsPanel(
                        uiState = uiState,
                        modifier =
                            Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        onInputCandidate = onInputCandidate,
                        onRevealSingleChar = onRevealSingleChar,
                        onRevealWholeIdiom = onRevealWholeIdiom,
                        onReplaySpeech = onReplaySpeech,
                        onToggleAutoSpeak = onToggleAutoSpeak,
                        onOpenNextLevel = onOpenNextLevel,
                    )
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(WordDragonDimensions.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(WordDragonDimensions.SectionSpacing),
                ) {
                    GameBoardCard(
                        uiState = uiState,
                        modifier = Modifier.fillMaxWidth(),
                        onSelectCell = onSelectCell,
                    )
                    GameControlsPanel(
                        uiState = uiState,
                        modifier = Modifier.fillMaxWidth(),
                        onInputCandidate = onInputCandidate,
                        onRevealSingleChar = onRevealSingleChar,
                        onRevealWholeIdiom = onRevealWholeIdiom,
                        onReplaySpeech = onReplaySpeech,
                        onToggleAutoSpeak = onToggleAutoSpeak,
                        onOpenNextLevel = onOpenNextLevel,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBoardCard(
    uiState: GameUiState,
    modifier: Modifier = Modifier,
    onSelectCell: (Int, Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val boardLayout =
            if (uiState.boardWidth > 0) {
                BoardLayoutCalculator.calculate(
                    availableWidthDp = maxWidth.value.toInt(),
                    boardWidth = uiState.boardWidth,
                )
            } else {
                null
            }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            if (boardLayout == null || uiState.boardHeight <= 0) {
                Column(
                    modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = uiState.levelTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                return@ElevatedCard
            }

            val cellsByCoordinate = uiState.boardCells.associateBy { it.row to it.col }
            val cellSize = boardLayout.cellSizeDp.dp
            val cellSpacing = boardLayout.cellSpacingDp.dp
            val boardContentWidth = boardLayout.requiredWidthDp(uiState.boardWidth).dp
            val boardContainerModifier =
                if (boardLayout.requiresHorizontalScroll) {
                    Modifier.horizontalScroll(rememberScrollState())
                } else {
                    Modifier
                }

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = boardLayout.horizontalPaddingDp.dp,
                        vertical = boardLayout.verticalPaddingDp.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = uiState.levelTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = boardContainerModifier.width(boardContentWidth),
                        verticalArrangement = Arrangement.spacedBy(cellSpacing),
                    ) {
                        repeat(uiState.boardHeight) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                            ) {
                                repeat(uiState.boardWidth) { col ->
                                    val cell = cellsByCoordinate[row to col]
                                    if (cell == null) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .size(cellSize)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        )
                                    } else {
                                        Surface(
                                            modifier =
                                                Modifier
                                                    .size(cellSize)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { onSelectCell(cell.row, cell.col) }
                                                    .semantics {
                                                        text = AnnotatedString(cell.text)
                                                        selected = cell.isFocused
                                                    }.testTag("cell-$row-$col"),
                                            color = cellContainerColor(cell),
                                            border =
                                                BorderStroke(
                                                    width = if (cell.isFocused) 3.dp else 2.dp,
                                                    color = cellBorderColor(cell),
                                                ),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = cell.text.ifBlank { " " },
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GameControlsPanel(
    uiState: GameUiState,
    modifier: Modifier = Modifier,
    onInputCandidate: (Char) -> Unit,
    onRevealSingleChar: () -> Unit,
    onRevealWholeIdiom: () -> Unit,
    onReplaySpeech: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onOpenNextLevel: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WordDragonDimensions.SectionSpacing),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "释义提示",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.currentExplanation.ifBlank { "请填写一个四字成语。" },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (uiState.noticeText != null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.noticeText,
                    modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "候选字盘",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    uiState.candidateStates.forEach { candidate ->
                        Button(
                            onClick = { onInputCandidate(candidate.char) },
                            enabled = candidate.isEnabled && !uiState.isCompleted,
                            modifier =
                                Modifier
                                    .widthIn(min = WordDragonDimensions.MinTouchTarget + 12.dp)
                                    .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                                    .testTag("candidate-${candidate.char}"),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = candidate.char.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = "余${candidate.remainingCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onRevealSingleChar,
                enabled = !uiState.isCompleted,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                        .testTag("action-hint-char"),
            ) {
                Text(text = "提示一字", style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onRevealWholeIdiom,
                enabled = !uiState.isCompleted,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                        .testTag("action-hint-idiom"),
            ) {
                Text(text = "揭示成语", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onReplaySpeech,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                        .testTag("action-replay"),
            ) {
                Text(text = "重播发音", style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(
                onClick = { onToggleAutoSpeak(!uiState.autoSpeakEnabled) },
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = WordDragonDimensions.PrimaryButtonHeight),
            ) {
                Text(
                    text = if (uiState.autoSpeakEnabled) "自动发音：开" else "自动发音：关",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (uiState.isCompleted) {
            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("game-complete-card"),
            ) {
                Column(
                    modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "通关成功",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "这一关已经保存完成，可以继续下一关。",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    uiState.nextLevelId?.let { nextLevelId ->
                        Button(
                            onClick = { onOpenNextLevel(nextLevelId) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                                    .testTag("action-next-level"),
                        ) {
                            Text(text = "进入下一关", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun cellContainerColor(cell: GameBoardCellUiState) =
    when {
        cell.isFocused -> MaterialTheme.colorScheme.primary
        cell.isSelected -> MaterialTheme.colorScheme.primaryContainer
        cell.isCorrect -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun cellBorderColor(cell: GameBoardCellUiState) =
    when {
        cell.isFocused -> MaterialTheme.colorScheme.primary
        cell.isSelected -> MaterialTheme.colorScheme.primary
        cell.isCorrect -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
