# ECN-0002: Word Dragon 输入约束与候选池字频修正

## 基本信息

- **ECN 编号**：ECN-0002
- **关联 PRD**：PRD-0001
- **关联 Req ID**：`REQ-0001-004`、`REQ-0001-005`、`REQ-0001-010`、`REQ-0001-011`
- **发现阶段**：`M3R` 真机安装后首关试玩
- **日期**：2026-04-01

## 变更原因

真机试玩最新 `debug` 包时，首关暴露出三类问题：

- `释义提示` 卡片仍占用关卡页主空间，不符合“每一寸都要斟酌”的目标。
- 首关真实内容需要重复使用 `一`、`两`、`制`，但候选池按去重字列表生成，导致字频不足。
- 当前玩法允许错字落盘，与常见成语填词游戏“错字拒绝写入”的规则不一致。

这些问题会直接造成误导输入、内容不可解和界面冗余，因此需要在 `v1` 内继续修正。

## 变更内容

### 原设计

- `REQ-0001-004`：允许候选字写入当前焦点格，只要候选池还有剩余次数即可。
- `REQ-0001-005`：关卡页保留显式释义区。
- `REQ-0001-010`：内容校验只检查“候选池是否包含某个字”，不检查频次是否足够。

### 新设计

- 关卡页进一步收缩：
  - 删除显式 `释义提示` 卡片。
  - 释义仅作为当前线索上下文，供自动朗读和手动重播使用。
- 输入规则进一步收紧：
  - 当前焦点格只接受正确字。
  - 错误候选字点击后不落盘、不扣次数，并播放本地错误音。
- 内容生成与校验进一步收紧：
  - 候选池按棋盘真实需求频次生成。
  - 校验脚本必须能检出频次覆盖不足的关卡。

## 影响范围

- **受影响的 Req ID**：
  - `REQ-0001-004`
  - `REQ-0001-005`
  - `REQ-0001-010`
  - `REQ-0001-011`
- **受影响的 v1 计划**：
  - `docs/plan/v1-index.md`
  - 新增 `docs/plan/v1-input-hardening.md`
- **受影响的测试**：
  - `GameSessionEngineTest`
  - `PlayLevelFlowTest`
  - `tools/content_pipeline/tests/test_level_generator.py`
  - `tools/content_pipeline/tests/test_validate_content.py`
- **受影响的代码文件**：
  - `GameSessionEngine.kt`
  - `GameSessionState.kt`
  - `GameViewModel.kt`
  - `GameScreen.kt`
  - `WordDragonDependencies.kt`
  - `tools/content_pipeline/src/word_dragon_content/level_generator.py`
  - `tools/content_pipeline/src/word_dragon_content/content_validator.py`

## 验证结果

- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"`：通过
- `.\gradlew.bat :app:testDebugUnitTest`：通过
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest`：通过
- `uv run pytest tools/content_pipeline/tests/test_level_generator.py tools/content_pipeline/tests/test_validate_content.py -q`：通过
- `uv run python tools/content_pipeline/scripts/build_levels.py --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8 --strict`：通过
- `uv run python tools/content_pipeline/scripts/validate_content.py --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels --strict`：通过
- `.\gradlew.bat :app:installDebug`：已将最新调试包装到设备 `QXNUT21B12002805`

## 处置方式

- [x] PRD 已同步更新（标注 ECN-0002）
- [x] `v1` 计划已同步更新
- [x] 追溯矩阵已同步更新
- [x] 相关测试已同步更新
