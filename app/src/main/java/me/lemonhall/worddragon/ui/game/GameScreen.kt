package me.lemonhall.worddragon.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    onSelectIdiom: (String) -> Unit,
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
            val isWide = maxWidth >= 760.dp
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
                        modifier = Modifier.weight(1f),
                    )
                    GameSidePanel(
                        uiState = uiState,
                        modifier = Modifier.weight(1f),
                        onSelectIdiom = onSelectIdiom,
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
                    GameBoardCard(uiState = uiState, modifier = Modifier.fillMaxWidth())
                    GameSidePanel(
                        uiState = uiState,
                        modifier = Modifier.fillMaxWidth(),
                        onSelectIdiom = onSelectIdiom,
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
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(WordDragonDimensions.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = uiState.levelTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val cellsByCoordinate = uiState.boardCells.associateBy { it.row to it.col }
            repeat(uiState.boardHeight) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(uiState.boardWidth) { col ->
                        val cell = cellsByCoordinate[row to col]
                        if (cell == null) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .size(WordDragonDimensions.MinTouchTarget)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            )
                        } else {
                            Surface(
                                modifier =
                                    Modifier
                                        .size(WordDragonDimensions.MinTouchTarget)
                                        .semantics {
                                            text = AnnotatedString(cell.text)
                                        }
                                        .testTag("cell-$row-$col"),
                                shape = RoundedCornerShape(16.dp),
                                color =
                                    when {
                                        cell.isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        cell.isCorrect -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                border =
                                    BorderStroke(
                                        width = 2.dp,
                                        color =
                                            when {
                                                cell.isSelected -> MaterialTheme.colorScheme.primary
                                                cell.isCorrect -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                    ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = cell.text,
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GameSidePanel(
    uiState: GameUiState,
    modifier: Modifier = Modifier,
    onSelectIdiom: (String) -> Unit,
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
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("selected-idiom-card"),
        ) {
            Column(
                modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "当前成语",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.selectedFilledText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = uiState.selectedExplanation,
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

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "成语列表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                uiState.idioms.forEach { idiom ->
                    OutlinedButton(
                        onClick = { onSelectIdiom(idiom.idiomId) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = WordDragonDimensions.PrimaryButtonHeight)
                                .testTag("idiom-${idiom.idiomId}"),
                        border =
                            BorderStroke(
                                width = 2.dp,
                                color =
                                    when {
                                        idiom.isSelected -> MaterialTheme.colorScheme.primary
                                        idiom.isSolved -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                            ),
                    ) {
                        Text(
                            text = idiom.filledText,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
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
                    uiState.candidateChars.forEach { candidate ->
                        Button(
                            onClick = { onInputCandidate(candidate) },
                            modifier =
                                Modifier
                                    .width(WordDragonDimensions.MinTouchTarget)
                                    .height(WordDragonDimensions.MinTouchTarget)
                                    .testTag("candidate-$candidate"),
                        ) {
                            Text(
                                text = candidate.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
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
    }
}
