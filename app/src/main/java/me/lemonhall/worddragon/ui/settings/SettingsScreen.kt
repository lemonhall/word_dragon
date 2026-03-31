package me.lemonhall.worddragon.ui.settings

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.lemonhall.worddragon.ui.theme.WordDragonDimensions

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    autoSpeakEnabled: Boolean,
    onAutoSpeakChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
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
            SettingCard(
                title = "大字号模式",
                summary = "基础字号已经放大，后续关卡格子会保持不少于 28sp。",
            )
            SettingCard(
                title = "自动发音",
                summary = if (autoSpeakEnabled) "当前会在选中成语时自动发音。" else "当前只保留手动重播发音。",
                trailing = {
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = onAutoSpeakChanged,
                    )
                },
            )
            SettingCard(
                title = "横竖屏支持",
                summary = "页面骨架已按可旋转布局设计，后续会补齐完整的旋转恢复验证。",
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    summary: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(WordDragonDimensions.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
            )
            trailing?.invoke()
        }
    }
}
