# v1 Input Hardening

## Goal

把关卡页进一步收缩到“棋盘 + 字盘 + 操作按钮”的核心形态，同时把玩法规则改成“错字拒绝写入”，并修复内容资产候选池字频不足的问题，避免出现不可解关卡。

## PRD Trace

- `REQ-0001-004`
- `REQ-0001-005`
- `REQ-0001-010`
- `REQ-0001-011`

## Scope

- 删除显式 `释义提示` 卡片
- 为错字输入增加拒绝逻辑和本地错误音
- 修复候选池剩余次数统计口径，只按正确已占用格扣减
- 修复内容生成器的候选池频次输出
- 扩展内容校验器，拦截字频覆盖不足的关卡
- 更新自动化测试并重新生成内容资产

## Out Of Scope

- 不修改章节结构和总关卡目标
- 不增加新的游戏模式、评分或经济系统
- 不在本计划中处理横竖屏旋转恢复遗留项

## Acceptance

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` 退出码为 `0`
- `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q` 退出码为 `0`
- `uv run pytest tools/content_pipeline/tests/test_validate_content.py -q` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/build_levels.py --strict --min-levels 1000` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/validate_content.py --strict` 退出码为 `0`
- 真机首关不再显示 `释义提示` 卡片
- 真机首关尝试点错字时，棋盘不落错字且会播放错误音
- 真机首关完整可解，不再出现 `两`、`一`、`制` 被提前耗尽
- 反作弊条款：如果只是把错字写入后再自动撤回、或只对首关手改资产而不修生成/校验规则，则本计划不得判为完成

## Files

- Create: `docs/superpowers/specs/2026-04-01-word-dragon-input-hardening-design.md`
- Create: `docs/ecn/ECN-0002-word-dragon-input-hardening.md`
- Create: `docs/plan/v1-input-hardening.md`
- Modify: `docs/prd/PRD-0001-word-dragon-v1.md`
- Modify: `docs/plan/v1-index.md`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionState.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionEngine.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameScreen.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/WordDragonDependencies.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/audio/ErrorSoundPlayer.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/audio/AndroidErrorSoundPlayer.kt`
- Create: `app/src/main/res/raw/error_reject.wav`
- Modify: `app/src/androidTest/java/me/lemonhall/worddragon/testsupport/FakeWordDragonDependencies.kt`
- Modify: `app/src/test/java/me/lemonhall/worddragon/domain/game/GameSessionEngineTest.kt`
- Modify: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/PlayLevelFlowTest.kt`
- Modify: `tools/content_pipeline/src/word_dragon_content/level_generator.py`
- Modify: `tools/content_pipeline/src/word_dragon_content/content_validator.py`
- Modify: `tools/content_pipeline/tests/test_level_generator.py`
- Modify: `tools/content_pipeline/tests/test_validate_content.py`
- Modify: `app/src/main/assets/content/levels/*.json`
- Modify: `app/src/main/assets/content/chapters.json`

## Steps

1. 先更新 `GameSessionEngineTest.kt`、`PlayLevelFlowTest.kt`、`test_level_generator.py`、`test_validate_content.py` 的失败用例，锁定错字拒绝、释义卡片移除和候选池频次约束。
2. 运行相关测试到红，预期失败原因是当前状态机允许错填、UI 仍显示释义卡片、内容管线仍按去重字输出候选池。
3. 实现领域状态机的错字拒绝和输入反馈状态，接入本地错误音播放抽象。
4. 实现关卡页 UI 收缩，删除 `释义提示` 卡片。
5. 实现内容生成器和校验器的候选池频次修复，并重生成内容资产。
6. 重新运行单元测试、仪器测试和内容管线测试，预期全部通过。
7. 安装最新 `debug` 包到真机，手工验证首关错字拒绝、错误音和候选池不再耗尽。
8. 更新 `docs/plan/v1-index.md` 的追溯矩阵和差异列表，记录本轮证据。

## Risks

- 错字拒绝后，候选池统计口径必须从“所有输入”切到“正确占用”，否则仍会出现字被误耗尽。
- 如果错误音播放抽象耦合到 Compose 或 TTS，会让测试和生命周期变脆。
- 内容资产必须由生成器和校验器一起修，单改现有 JSON 会在下一次生成时回退。

## Evidence

- `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` 通过。
- `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest` 通过，说明 `HintPolicyTest` 等回归未被这轮改动打坏。
- `2026-04-01`：`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest` 通过。
- `2026-04-01`：`uv run pytest tools/content_pipeline/tests/test_level_generator.py tools/content_pipeline/tests/test_validate_content.py -q` 通过。
- `2026-04-01`：`uv run python tools/content_pipeline/scripts/build_levels.py --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8 --strict` 生成 `1000` 关。
- `2026-04-01`：`uv run python tools/content_pipeline/scripts/validate_content.py --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels --strict` 通过，首关候选池已按真实棋盘频次展开。
- `2026-04-01`：`.\gradlew.bat :app:installDebug` 已把最新 `debug` 包安装到设备 `QXNUT21B12002805`，供真机手验。
