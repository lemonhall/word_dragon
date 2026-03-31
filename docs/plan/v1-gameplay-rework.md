# v1 Gameplay Rework

## Goal

把现有关卡页从“成语列表驱动”重构为“棋盘驱动”，补齐焦点格、候选字剩余次数灰态、无答案 TTS 和本地恢复焦点状态，使玩法回到常见成语填词的主交互模型。

## PRD Trace

- `REQ-0001-004`
- `REQ-0001-005`
- `REQ-0001-006`
- `REQ-0001-007`
- `REQ-0001-011`

## Scope

- 重构领域状态机，加入当前焦点格、棋盘点击选词和候选字剩余次数统计
- 重构关卡页 UI，删除 `当前成语` / `成语列表`
- 重构 TTS 文案策略，使其只播释义提示和状态反馈
- 扩展本地快照，恢复焦点格与当前选中成语
- 更新自动化测试与真机验证链路

## Out Of Scope

- 不重做章节页、首页和内容管线
- 不在本计划中引入手写输入、拼音输入或联网能力
- 不在本计划中解决所有横竖屏和布局验证遗留项，那部分仍由 `v1-verification-content-pack` 负责

## Acceptance

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest` 退出码为 `0`
- 真机手工验证首关时，页面不再出现 `当前成语` 和 `成语列表`
- 真机手工验证首关时，点击棋盘格即可改变当前输入焦点
- 真机手工验证首关时，候选字用尽后会置灰且不可点
- 反作弊条款：如果仅隐藏旧面板、保留旧的“按成语列表选词”内部路径，或 TTS 只是关闭自动朗读但手动重播仍读答案，则本计划不得判为完成

## Files

- Modify: `docs/prd/PRD-0001-word-dragon-v1.md`
- Create: `docs/ecn/ECN-0001-word-dragon-gameplay-rework.md`
- Modify: `docs/plan/v1-index.md`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionState.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionEngine.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatter.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/data/progress/ProgressStore.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameScreen.kt`
- Modify: `app/src/test/java/me/lemonhall/worddragon/domain/game/GameSessionEngineTest.kt`
- Modify: `app/src/test/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatterTest.kt`
- Modify: `app/src/test/java/me/lemonhall/worddragon/data/progress/ProgressStoreTest.kt`
- Create: `app/src/test/java/me/lemonhall/worddragon/ui/game/CandidateInventoryTest.kt`
- Modify: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/PlayLevelFlowTest.kt`
- Modify: `app/src/androidTest/java/me/lemonhall/worddragon/e2e/ContinueGameFlowTest.kt`

## Steps

1. 先写并更新 `GameSessionEngineTest.kt`、`GameSpeechFormatterTest.kt`、`ProgressStoreTest.kt` 的失败用例，覆盖棋盘点击选中焦点、交叉格切换、候选字剩余次数、焦点恢复和 TTS 禁止朗读答案。
2. 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest" --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest" --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"`，预期失败原因是领域状态、存档字段和 TTS 文案仍是旧模型。
3. 实现 `GameSessionState.kt`、`GameSessionEngine.kt`、`GameSpeechFormatter.kt`、`ProgressStore.kt` 的最小改动，让纯领域/存档/TTS 测试先转绿。
4. 再次运行上述单元测试命令，预期全部通过。
5. 编写或更新 `PlayLevelFlowTest.kt` 与 `ContinueGameFlowTest.kt` 的失败场景，覆盖“页面不再出现旧面板”“点击棋盘格切换焦点”“候选字灰态”“重进恢复焦点”。
6. 运行 `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest`，预期失败原因是 `GameViewModel` 和 `GameScreen` 仍输出旧 UI 结构和旧交互。
7. 实现 `GameViewModel.kt` 与 `GameScreen.kt` 的最小 UI 重构，删除旧面板、接通棋盘点击与候选字灰态，并保持提示按钮、下一关和自动发音开关继续可用。
8. 再次运行上述仪器测试命令，预期通过。
9. 运行 `.\gradlew.bat :app:installDebug` 把最新调试包装到真机，手工验证首关：点击棋盘、观察灰态、点击发音确认不读答案。
10. 更新 `docs/plan/v1-index.md` 的追溯矩阵与差异列表，记录本轮证据和剩余风险。

## Risks

- 焦点格和交叉格切换属于纯玩法核心，如果规则落在 UI 层而不是领域层，后续会再次漂移。
- 候选字剩余次数与覆盖输入的联动容易出错，必须用测试锁住“旧字归还、新字扣减”的细节。
- TTS 去答案化后，若文案降级策略不清晰，可能出现“什么都不播”的无效体验，因此要给出释义为空时的退化反馈。
