package me.lemonhall.worddragon.ui.chapters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private data class ChapterPreview(
    val title: String,
    val summary: String,
)

private val demoChapters =
    listOf(
        ChapterPreview("第一章 常见开门词", "120 关，先熟悉高频四字成语。"),
        ChapterPreview("第二章 日常好搭配", "120 关，控制每关最多 8 个词条。"),
        ChapterPreview("第三章 朗读练耳", "120 关，为后续 TTS 玩法预留入口。"),
    )

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChapterListScreen(
    banner: String,
    onBack: () -> Unit,
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
                    text = banner,
                    modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            demoChapters.forEach { chapter ->
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = chapter.summary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
