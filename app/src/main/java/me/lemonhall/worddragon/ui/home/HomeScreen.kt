package me.lemonhall.worddragon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.lemonhall.worddragon.ui.theme.WordDragonDimensions

@Composable
fun HomeScreen(
    onContinueGame: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WordDragonDimensions.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(WordDragonDimensions.SectionSpacing),
    ) {
        Text(
            text = "成语接龙",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "离线大字版，慢慢玩，不着急。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(WordDragonDimensions.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "今天建议",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "先从常用成语开始，熟悉大字格子和候选字盘的节奏。",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        EntryButton(
            text = "继续游戏",
            subtitle = "从上次保存的位置继续；如果还没有进度，就从第一章开始。",
            onClick = onContinueGame,
        )
        EntryButton(
            text = "章节选关",
            subtitle = "按章节慢慢挑选，单关成语数量会控制在大字显示范围内。",
            onClick = onOpenChapters,
        )
        EntryButton(
            text = "设置",
            subtitle = "查看字号、发音和横竖屏说明。",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun EntryButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = WordDragonDimensions.PrimaryButtonHeight),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
