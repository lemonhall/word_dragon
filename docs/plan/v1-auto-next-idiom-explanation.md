# v1 Auto Next Idiom Explanation

## Goal

补齐“当前成语完成后系统自动切到下一条成语时，立即朗读新成语释义”的语音节拍，让新的填词工作在视觉和听觉上同步开始。

## PRD Trace

- `REQ-0001-006`
- `REQ-0001-011`

## Scope

- 在 `GameViewModel` 基于前后会话差异判断“是否自动切到新的未完成成语”
- 保留现有完成答案播报，并在其后补播新成语释义
- 为该播报顺序增加回归测试

## Out Of Scope

- 不改 `GameSessionEngine` 的选中与焦点推进逻辑
- 不改 `GameSpeechFormatter` 的释义和答案格式
- 不新增新的 TTS 开关或设置项

## Acceptance

- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.GameViewModelSpeechFlowTest` 退出码为 `0`
- 手工填完当前成语并自动切到下一条时，播报顺序为“当前成语初始释义 → 当前成语答案 → 下一条成语释义”
- `揭示成语` 让当前成语完成并自动切到下一条时，也满足相同播报顺序
- 关卡直接通关时，不会伪造一条不存在的“下一条释义”播报
- 反作弊条款：如果只保留完成答案播报而没有下一条释义，或把下一条释义提前到答案播报前，本计划不得判为完成

## Files

- Create: `docs/superpowers/specs/2026-04-01-word-dragon-auto-next-idiom-explanation-design.md`
- Create: `docs/ecn/ECN-0004-word-dragon-auto-next-idiom-explanation.md`
- Create: `docs/plan/v1-auto-next-idiom-explanation.md`
- Modify: `docs/prd/PRD-0001-word-dragon-v1.md`
- Modify: `docs/plan/v1-index.md`
- Create: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/GameViewModelSpeechFlowTest.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`

## Steps

1. 先新增 `GameViewModelSpeechFlowTest.kt`，把“自动切到下一条成语时补播释义”的顺序写成红测。
2. 运行目标仪器测试到红，确认失败原因是当前版本只播答案、不播下一条释义。
3. 在 `GameViewModel.kt` 中基于 `selectedIdiomId` 变化和未完成状态补充新的释义播报条件。
4. 重新运行目标仪器测试到绿。
5. 运行现有 `PlayLevelFlowTest` 回归，确保单成语关卡和既有完成播报不被破坏。
6. 更新 `docs/plan/v1-index.md` 和 `ECN-0004` 的验证结果。

## Risks

- 一次操作同时完成成语并切换选中对象，播报顺序必须先答案后释义，不能反过来。
- 如果判断条件过宽，最后一关通关时可能产生多余播报。

## Evidence

- `2026-04-01`：`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.GameViewModelSpeechFlowTest` 通过。
- `2026-04-01`：`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` 通过。
- `2026-04-01`：`GameViewModelSpeechFlowTest` 已验证手工填完和 `揭示成语` 自动切到下一条时，播报顺序均为“当前释义 → 当前答案 → 下一条释义”。
- `2026-04-01`：`.\gradlew.bat :app:installDebug` 已把最新 `debug` 包安装到设备 `RTE-AL00 - 12`。
