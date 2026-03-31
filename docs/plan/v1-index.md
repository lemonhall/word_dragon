# v1 Index

## 愿景

- PRD：`docs/prd/PRD-0001-word-dragon-v1.md`
- 设计文档：`docs/superpowers/specs/2026-03-31-word-dragon-design.md`
- 版本目标：先完成 `v1` 的离线单机闭环，再进入后续迭代。

## 里程碑

| 里程碑 | 范围 | DoD | 验证命令 / 测试 | 状态 |
|---|---|---|---|---|
| `M1` Bootstrap Foundation | Android 项目骨架、首页、章节页骨架、老年友好主题基线 | `.\gradlew.bat :app:assembleDebug` 退出码为 `0`；`HomeLaunchTest` 和 `WordDragonTypographyTest` 通过；首页存在 `继续游戏`、`章节选关`、`设置` 三个真实入口而不是占位文案 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest` | done |
| `M2` Content Pipeline | 常用四字成语词库、固定关卡包、章节索引、本地内容校验脚本 | 内容构建脚本输出 `idiom_catalog.json`、`chapters.json`、关卡包；内容校验脚本返回 `0`；总关卡数 `>= 1000`；任何关卡成语数不超过 `8` | `uv run python tools/content_pipeline/scripts/build_catalog.py --strict`；`uv run python tools/content_pipeline/scripts/build_levels.py --strict --min-levels 1000`；`uv run python tools/content_pipeline/scripts/validate_content.py --strict` | done |
| `M3` Gameplay Loop | 关卡页、候选字盘、提示、TTS、自动存档、继续游戏与选关 | `GameSessionEngineTest`、`ProgressStoreTest`、`GameSpeechFormatterTest` 通过；`PlayLevelFlowTest` 通过；关卡页不会弹系统输入法；TTS 失败不阻断通关 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"`；`.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` | todo |
| `M4` Verification And Content Pack | 横竖屏适配、继续游戏 E2E、内容资源验收、发布前验证 | 旋转恢复和继续游戏流程自动化通过；内容布局校验脚本通过；`assembleDebug` 通过；不存在断链关卡和非法字号布局；无法通过只改文案或空壳界面冒充完成 | `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --min-cell-sp 28 --min-touch-dp 56`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest,me.lemonhall.worddragon.e2e.RotateAndResumeTest`；`.\gradlew.bat :app:assembleDebug` | todo |

## 计划索引

- `docs/plan/v1-bootstrap-foundation.md`
- `docs/plan/v1-content-pipeline.md`
- `docs/plan/v1-gameplay-loop.md`
- `docs/plan/v1-verification-content-pack.md`

## 追溯矩阵

| Req ID | PRD | v1 Plan | 单元 / 集成测试 | E2E / 端到端 | 计划验证命令 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|
| `REQ-0001-001` | `PRD-0001 §REQ-0001-001` | `v1-bootstrap-foundation`、`v1-gameplay-loop` | `HomeLaunchTest` | `ContinueGameFlowTest` | `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest` | `2026-03-31`：`HomeLaunchTest` 通过；真机首页截图位于 `build/verification/word_dragon_home.png` | doing |
| `REQ-0001-002` | `PRD-0001 §REQ-0001-002` | `v1-content-pipeline` | `tools/content_pipeline/tests/test_catalog_filter.py` | — | `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q` | `2026-03-31`：`idiom_catalog.json` 生成 `36084` 条启用词条，全部为四字成语 | doing |
| `REQ-0001-003` | `PRD-0001 §REQ-0001-003` | `v1-content-pipeline`、`v1-gameplay-loop` | `tools/content_pipeline/tests/test_level_generator.py` | `PlayLevelFlowTest` | `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q` | `2026-03-31`：生成 `1000` 关固定关卡，当前内容包采用 `chain-4` 连通模板 | doing |
| `REQ-0001-004` | `PRD-0001 §REQ-0001-004` | `v1-gameplay-loop` | `GameSessionEngineTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` | 待实现 | todo |
| `REQ-0001-005` | `PRD-0001 §REQ-0001-005` | `v1-gameplay-loop` | `HintPolicyTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.HintPolicyTest"` | 待实现 | todo |
| `REQ-0001-006` | `PRD-0001 §REQ-0001-006` | `v1-gameplay-loop` | `GameSpeechFormatterTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` | 待实现 | todo |
| `REQ-0001-007` | `PRD-0001 §REQ-0001-007` | `v1-bootstrap-foundation`、`v1-gameplay-loop` | `ProgressStoreTest` | `ContinueGameFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"` | 待实现 | todo |
| `REQ-0001-008` | `PRD-0001 §REQ-0001-008` | `v1-bootstrap-foundation`、`v1-verification-content-pack` | `WordDragonTypographyTest`、`tools/content_pipeline/tests/test_layout_validator.py` | `RotateAndResumeTest` | `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --min-cell-sp 28 --min-touch-dp 56` | `2026-03-31`：`WordDragonTypographyTest` 通过；首页和主按钮已按大字号基线实现 | doing |
| `REQ-0001-009` | `PRD-0001 §REQ-0001-009` | `v1-content-pipeline`、`v1-verification-content-pack` | `tools/content_pipeline/tests/test_level_generator.py` | — | `uv run python tools/content_pipeline/scripts/build_levels.py --strict --min-levels 1000` | `2026-03-31`：`chapters.json` 统计 `20` 章、`1000` 关 | doing |
| `REQ-0001-010` | `PRD-0001 §REQ-0001-010` | `v1-content-pipeline` | `tools/content_pipeline/tests/test_validate_content.py` | — | `uv run python tools/content_pipeline/scripts/validate_content.py --strict` | `2026-03-31`：`research/content/content_provenance.md` 已记录来源、筛选与生成时间 | done |
| `REQ-0001-011` | `PRD-0001 §REQ-0001-011` | `v1-gameplay-loop`、`v1-verification-content-pack` | `GameSessionEngineTest`、`ProgressStoreTest` | `ContinueGameFlowTest`、`RotateAndResumeTest` | `.\gradlew.bat :app:testDebugUnitTest`；`.\gradlew.bat :app:connectedDebugAndroidTest` | 待实现 | todo |

## ECN 索引

- 当前无 ECN。

## DoD 硬度自检

- 每个里程碑 DoD 都是二元判定或量化约束，没有软性表述。
- 每个里程碑都绑定了可重复的命令或测试路径。
- 每个里程碑都带反作弊条款，防止空界面、空数据或只改文案冒充完成。
- 所有计划文档都明确了本轮不做什么，避免范围漂移。

## 文档完整性自检

- `PRD-0001` 中所有需求均有 `Req ID`、范围、非目标、验收口径与阶段归属。
- `v1-*` 计划文档均已映射对应 `Req ID`。
- `v1-*` 计划文档中的验收条目均绑定命令和预期结果。
- 术语统一使用“成语词条 / 关卡 / 章节 / 候选字盘 / 进度快照”。

## 差异列表

- `M1` 已完成：`.\gradlew.bat :app:assembleDebug`、`WordDragonTypographyTest`、`HomeLaunchTest` 已通过，并已完成一次真机首页截图验收。
- `M2` 已完成：离线内容管线、词库筛选、`1000` 关生成与内容校验已落盘。
- 继续游戏的真实进度恢复、章节解锁与自动存档仍待 `v1-gameplay-loop` 实现。
- 横竖屏自动化恢复验证和内容布局校验仍待 `v1-verification-content-pack` 实现。
