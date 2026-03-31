# ECN-0003: Word Dragon 成语完成播报与紧凑布局

## 基本信息

- **ECN 编号**：ECN-0003
- **关联 PRD**：PRD-0001
- **关联 Req ID**：`REQ-0001-006`、`REQ-0001-008`、`REQ-0001-011`
- **发现阶段**：`M3H` 完成后的继续真机试玩
- **日期**：2026-04-01

## 变更原因

输入加固完成后，真机试玩继续暴露两点体验差异：

- 一条四字成语完成时没有直接播报答案，完成反馈不够明确。
- 关卡页仍有可进一步压缩的重复信息，页面在部分手机上仍偏松散。

## 变更内容

### 原设计

- `REQ-0001-006` 只要求点选成语时朗读释义，不在完成成语时朗读答案。
- `REQ-0001-008` 只约束最小字号和最小触控尺寸，没有把关卡页信息密度继续收紧写成硬要求。

### 新设计

- 任意一条成语首次完成时，自动朗读一次该成语答案本身。
- 三种完成路径都算触发：手工填完、`提示一字` 补完、`揭示成语` 补完。
- 关卡页顶部改为 `第00xx级`，移除重复关卡信息与 `候选字盘` 标题，并收紧游戏页局部留白与按钮尺寸。

## 影响范围

- **受影响的 Req ID**：
  - `REQ-0001-006`
  - `REQ-0001-008`
  - `REQ-0001-011`
- **受影响的 v1 计划**：
  - `docs/plan/v1-index.md`
  - 新增 `docs/plan/v1-completion-speech-and-compact-layout.md`
- **受影响的测试**：
  - `GameSpeechFormatterTest`
  - `PlayLevelFlowTest`
- **受影响的代码文件**：
  - `GameViewModel.kt`
  - `GameScreen.kt`
  - `GameSpeechFormatter.kt`
  - `DimensionTokens.kt` 或游戏页局部布局参数

## 处置方式

- [x] PRD 已同步更新（标注 ECN-0003）
- [x] `v1` 计划已同步更新
- [x] 追溯矩阵已同步更新
- [x] 相关测试已同步更新

## 验证结果

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"`：通过
- `.\gradlew.bat :app:testDebugUnitTest`：通过
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest`：通过
- `.\gradlew.bat :app:installDebug`：已将最新调试包装到设备 `QXNUT21B12002805`
