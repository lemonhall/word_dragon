package me.lemonhall.worddragon.ui.chapters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.lemonhall.worddragon.ui.theme.WordDragonDimensions

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun ChapterListScreen(
    uiState: ChapterListUiState,
    onBack: () -> Unit,
    onOpenLevel: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "章节选关",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "返回",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(WordDragonDimensions.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(WordDragonDimensions.SectionSpacing),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = uiState.banner,
                    modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            uiState.chapters.forEach { chapter ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = chapter.summary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            chapter.levelEntries.forEach { level ->
                                val buttonModifier =
                                    Modifier
                                        .heightIn(min = WordDragonDimensions.MinTouchTarget)
                                        .fillMaxWidth(0.22f)
                                if (level.isUnlocked) {
                                    Button(
                                        onClick = { onOpenLevel(level.levelId) },
                                        modifier = buttonModifier,
                                    ) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = buttonModifier,
                                    ) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.bodyLarge,
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
