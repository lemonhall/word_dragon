# ECN-0004: Word Dragon 自动切换下一成语时补播释义

## 基本信息

- **ECN 编号**：ECN-0004
- **关联 PRD**：PRD-0001
- **关联 Req ID**：`REQ-0001-006`、`REQ-0001-011`
- **发现阶段**：`M3C` 完成后的继续真机试玩
- **日期**：2026-04-01

## 变更原因

`ECN-0003` 补上“成语完成播报”后，真机继续试玩发现另一个节拍缺口：当系统因为当前成语完成而自动选中下一条未完成成语时，新的填词工作已经开始，但语音没有同步播出这条新成语的释义，用户只能再点击一次棋盘才听得到。

## 变更内容

### 原设计

- `REQ-0001-006` 只要求点选成语时自动朗读释义，以及在成语首次完成时自动朗读答案。
- 自动切换到下一条未完成成语时，没有额外释义播报。

### 新设计

- 当一次操作让系统自动切到下一条未完成成语时，在保留当前成语答案播报的前提下，紧接着自动朗读一次新成语释义。
- 触发路径包括手工填字、`提示一字`、`揭示成语` 造成的自动切换。
- `重播发音` 语义不变，仍然只重播当前释义。

## 影响范围

- **受影响的 Req ID**：
  - `REQ-0001-006`
  - `REQ-0001-011`
- **受影响的 v1 计划**：
  - `docs/plan/v1-index.md`
  - 新增 `docs/plan/v1-auto-next-idiom-explanation.md`
- **受影响的测试**：
  - `GameViewModelSpeechFlowTest`
- **受影响的代码文件**：
  - `GameViewModel.kt`

## 处置方式

- [x] PRD 已同步更新（标注 ECN-0004）
- [x] `v1` 计划已同步更新
- [x] 追溯矩阵已同步更新
- [x] 相关测试已同步更新

## 验证结果

- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.GameViewModelSpeechFlowTest`：通过
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest`：通过
- `.\gradlew.bat :app:installDebug`：已将最新调试包装到设备 `RTE-AL00 - 12`
