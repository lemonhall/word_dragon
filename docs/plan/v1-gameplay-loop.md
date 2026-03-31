# v1 Gameplay Loop

## Goal

完成可玩的关卡闭环：章节选关、候选字盘、提示、自动朗读、自动存档、通关解锁和继续游戏。

## PRD Trace

- `REQ-0001-001`
- `REQ-0001-003`
- `REQ-0001-004`
- `REQ-0001-005`
- `REQ-0001-006`
- `REQ-0001-007`

## Scope

- 读取内置关卡与章节索引
- 实现关卡状态机、候选字盘交互、提示和通关判定
- 实现本地进度快照与继续游戏
- 接入 Android TTS 自动朗读与重播

## Out Of Scope

- 不实现在线排行和登录
- 不实现动态关卡下载
- 不实现复杂商业化机制或多货币系统

## Acceptance

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.HintPolicyTest"` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest` 退出码为 `0`
- 反作弊条款：如果关卡页仍依赖系统输入法、继续游戏无法恢复盘面、或 TTS 初始化失败就导致流程崩溃，则本计划不得判为完成

## Files

- Create: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetIdiomCatalogDataSource.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetLevelPackDataSource.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/data/progress/ProgressStore.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionState.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionEngine.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/HintPolicy.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatter.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/tts/AndroidTtsSpeaker.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/tts/TtsSpeaker.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/chapters/ChapterListViewModel.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/game/GameSessionEngineTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/game/HintPolicyTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/data/progress/ProgressStoreTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatterTest.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/PlayLevelFlowTest.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/e2e/ContinueGameFlowTest.kt`

## Steps

1. 先写 `GameSessionEngineTest`、`HintPolicyTest`、`ProgressStoreTest`、`GameSpeechFormatterTest` 四组失败测试，覆盖填字、提示、恢复进度和朗读文案。
2. 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"`，预期失败原因是领域模型和数据存储尚未实现。
3. 实现 `GameSessionState.kt`、`GameSessionEngine.kt`、`HintPolicy.kt` 和 `GameSpeechFormatter.kt` 的最小逻辑，先让纯领域测试转绿。
4. 再次运行上述四个单元测试命令，预期全部通过。
5. 编写 `PlayLevelFlowTest` 和 `ContinueGameFlowTest` 两个失败仪器测试，覆盖首页进入关卡、候选字盘填字通关、退出恢复盘面。
6. 运行 `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest`，预期失败原因是 UI、ViewModel 和数据源尚未接通。
7. 实现 `AssetIdiomCatalogDataSource.kt`、`AssetLevelPackDataSource.kt`、`ProgressStore.kt`、`GameViewModel.kt`、`GameScreen.kt`、`HomeViewModel.kt`、`ChapterListViewModel.kt` 与 `AndroidTtsSpeaker.kt`，接通完整流程。
8. 再次运行 `PlayLevelFlowTest` 与 `ContinueGameFlowTest`，预期通过。
9. 补做必要重构，把 TTS 抽象为 `TtsSpeaker` 接口，保证测试中可替换为 fake 实现且继续保持全部测试通过。
10. 手工辅助验证一次：在设备上关闭网络、完成一关、切到后台并重新进入，确认能继续游戏且不会弹系统输入法。

## Risks

- 关卡页状态较复杂，若 ViewModel 持有太多 UI 细节，后续旋转恢复和测试会变脆。
- TTS 与 UI 同步时序容易造成 flaky 测试，需要从一开始就抽象 speaker 接口并提供 fake 实现。
