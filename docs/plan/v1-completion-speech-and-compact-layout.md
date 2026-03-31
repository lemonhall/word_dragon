# v1 Completion Speech And Compact Layout

## Goal

让每条四字成语在首次完成时都能被清晰读出，同时继续压缩关卡页，把顶部与候选区中可省掉的信息拿掉，尽量提升一屏可见内容。

## PRD Trace

- `REQ-0001-006`
- `REQ-0001-008`
- `REQ-0001-011`

## Scope

- 为手工填完、`提示一字` 补完、`揭示成语` 补完三种路径增加“完成即播答案”
- 将关卡页标题改为 `第00xx级`
- 移除棋盘卡片内重复关卡信息
- 移除 `候选字盘` 标题
- 收紧游戏页局部间距与候选字按钮尺寸

## Out Of Scope

- 不修改 `重播发音` 的语义
- 不新增新的 TTS 设置项
- 不把所有机型都强行做成绝对一屏

## Acceptance

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` 退出码为 `0`
- 真机上手工填完一条成语后，会自动朗读 `成语文本。`
- 真机上 `提示一字` 或 `揭示成语` 把一条成语补完时，也会自动朗读 `成语文本。`
- 关卡页不再出现 `关卡练习`、`候选字盘`、`chapter-001 / level-0001`
- 关卡页顶部显示 `第00xx级`
- 反作弊条款：如果只是把手工完成接上答案播报，但两个提示路径不播，或只改顶部标题而保留重复关卡信息与候选区标题，本计划不得判为完成

## Files

- Create: `docs/superpowers/specs/2026-04-01-word-dragon-completion-speech-compact-layout-design.md`
- Create: `docs/ecn/ECN-0003-word-dragon-completion-speech-and-compact-layout.md`
- Create: `docs/plan/v1-completion-speech-and-compact-layout.md`
- Modify: `docs/prd/PRD-0001-word-dragon-v1.md`
- Modify: `docs/plan/v1-index.md`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatter.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameScreen.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/theme/DimensionTokens.kt` 或游戏页局部布局参数
- Modify: `app/src/test/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatterTest.kt`
- Modify: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/PlayLevelFlowTest.kt`

## Steps

1. 先修改 `GameSpeechFormatterTest.kt` 和 `PlayLevelFlowTest.kt`，把“完成后播答案”和“新标题/去候选区标题”的预期写成红测。
2. 运行相关测试到红，确认失败原因是当前只播释义且页面仍显示旧文案。
3. 在 `GameSpeechFormatter.kt` 新增答案播报格式化方法。
4. 在 `GameViewModel.kt` 里基于前后状态差异识别“新完成成语”，并在三种完成路径上触发答案播报。
5. 在 `GameScreen.kt` 压缩顶部和候选区，改为 `第00xx级`，删掉重复标题与 `候选字盘` 文案，并收紧局部间距。
6. 重新运行单元测试和仪器测试到绿。
7. 安装最新 `debug` 包到真机，手验完成播报和页面紧凑度。
8. 更新 `docs/plan/v1-index.md` 的追溯矩阵和差异列表，记录本轮证据。

## Risks

- 一次操作可能同时完成多条成语，完成播报必须基于“新完成集合”而不是当前选中成语单点判断。
- 页面压缩不能把候选字点按区缩得过小，否则会反向伤害老年用户体验。

## Evidence

- `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` 通过。
- `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest` 通过。
- `2026-04-01`：`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest` 通过。
- `2026-04-01`：`PlayLevelFlowTest` 已覆盖三种完成路径的答案播报，以及 `第0001级` / 去 `候选字盘` / 去旧关卡文案。
- `2026-04-01`：`.\gradlew.bat :app:installDebug` 已把最新 `debug` 包安装到设备 `QXNUT21B12002805`。
