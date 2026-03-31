# v1 Index

## 愿景

- PRD：`docs/prd/PRD-0001-word-dragon-v1.md`
- 设计文档：`docs/superpowers/specs/2026-03-31-word-dragon-design.md`
- 玩法重构设计：`docs/superpowers/specs/2026-04-01-word-dragon-gameplay-rework-design.md`
- 完成播报与紧凑布局设计：`docs/superpowers/specs/2026-04-01-word-dragon-completion-speech-compact-layout-design.md`
- 版本目标：先完成 `v1` 的离线单机闭环，再进入后续迭代。

## 里程碑

| 里程碑 | 范围 | DoD | 验证命令 / 测试 | 状态 |
|---|---|---|---|---|
| `M1` Bootstrap Foundation | Android 项目骨架、首页、章节页骨架、老年友好主题基线 | `.\gradlew.bat :app:assembleDebug` 退出码为 `0`；`HomeLaunchTest` 和 `WordDragonTypographyTest` 通过；首页存在 `继续游戏`、`章节选关`、`设置` 三个真实入口而不是占位文案 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest` | done |
| `M2` Content Pipeline | 常用四字成语词库、固定关卡包、章节索引、本地内容校验脚本 | 内容构建脚本输出 `idiom_catalog.json`、`chapters.json`、关卡包；内容校验脚本返回 `0`；总关卡数 `>= 1000`；任何关卡成语数不超过 `8` | `uv run python tools/content_pipeline/scripts/build_catalog.py --strict`；`uv run python tools/content_pipeline/scripts/build_levels.py --strict --min-levels 1000`；`uv run python tools/content_pipeline/scripts/validate_content.py --strict` | done |
| `M3` Gameplay Loop | 关卡页、候选字盘、提示、TTS、自动存档、继续游戏与选关 | `GameSessionEngineTest`、`HintPolicyTest`、`ProgressStoreTest`、`GameSpeechFormatterTest` 通过；`PlayLevelFlowTest`、`ContinueGameFlowTest` 通过；关卡页只使用候选字按钮，不依赖系统输入法；TTS 初始化失败时仅提示、不阻断通关 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest" --tests "me.lemonhall.worddragon.domain.game.HintPolicyTest" --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest" --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest` | done |
| `M3R` Gameplay Rework | 棋盘驱动交互、焦点格、候选字灰态、TTS 去答案化、恢复焦点状态 | 棋盘点击可选中成语和焦点格；页面不再出现 `当前成语` / `成语列表`；候选字按剩余次数即时置灰；TTS 在任何时机都不朗读答案；恢复未完成盘面时可恢复焦点与选中成语；无法通过保留旧面板、假灰态或仅关闭自动朗读来冒充完成 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest" --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest" --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest` | done |
| `M3H` Input Hardening | 去释义卡片、错字拒绝写入、本地错误音、候选池字频覆盖修正 | 页面不再出现 `释义提示` 卡片；错字点击不会落盘且会触发错误音；候选池只按正确占用扣减；内容生成和校验都能拦截候选池字频不足；无法通过仅手改首关 JSON 或错字写入后自动回滚来冒充完成 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest`；`uv run pytest tools/content_pipeline/tests/test_level_generator.py -q`；`uv run pytest tools/content_pipeline/tests/test_validate_content.py -q`；`uv run python tools/content_pipeline/scripts/validate_content.py --strict` | done |
| `M3C` Completion Speech And Compact Layout | 成语完成播报、顶部改为 `第00xx级`、去候选区标题、继续压缩关卡页 | 三种成语完成路径都会自动播报答案；页面不再出现 `关卡练习`、`候选字盘` 和重复关卡文案；顶部显示 `第00xx级`；无法通过只补手工播报或只改标题文字来冒充完成 | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest` | done |
| `M4` Verification And Content Pack | 横竖屏适配、继续游戏 E2E、内容资源验收、发布前验证 | 旋转恢复和继续游戏流程自动化通过；内容布局校验脚本通过；`assembleDebug` 通过；不存在断链关卡和非法字号布局；无法通过只改文案或空壳界面冒充完成 | `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --min-cell-sp 28 --min-touch-dp 56`；`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest,me.lemonhall.worddragon.e2e.RotateAndResumeTest`；`.\gradlew.bat :app:assembleDebug` | todo |

## 计划索引

- `docs/plan/v1-bootstrap-foundation.md`
- `docs/plan/v1-content-pipeline.md`
- `docs/plan/v1-gameplay-loop.md`
- `docs/plan/v1-gameplay-rework.md`
- `docs/plan/v1-input-hardening.md`
- `docs/plan/v1-completion-speech-and-compact-layout.md`
- `docs/plan/v1-verification-content-pack.md`

## 追溯矩阵

| Req ID | PRD | v1 Plan | 单元 / 集成测试 | E2E / 端到端 | 计划验证命令 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|
| `REQ-0001-001` | `PRD-0001 §REQ-0001-001` | `v1-bootstrap-foundation`、`v1-gameplay-loop` | `HomeLaunchTest` | `ContinueGameFlowTest` | `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest` | `2026-03-31`：`HomeLaunchTest` 通过；真机首页截图位于 `build/verification/word_dragon_home.png` | doing |
| `REQ-0001-002` | `PRD-0001 §REQ-0001-002` | `v1-content-pipeline` | `tools/content_pipeline/tests/test_catalog_filter.py` | — | `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q` | `2026-03-31`：`idiom_catalog.json` 生成 `36084` 条启用词条，全部为四字成语 | doing |
| `REQ-0001-003` | `PRD-0001 §REQ-0001-003` | `v1-content-pipeline`、`v1-gameplay-loop`、`v1-input-hardening` | `tools/content_pipeline/tests/test_level_generator.py` | `PlayLevelFlowTest` | `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q` | `2026-03-31`：生成 `1000` 关固定关卡，当前内容包采用 `chain-4` 连通模板；`2026-04-01`：候选池已按真实棋盘频次重生成，`uv run python tools/content_pipeline/scripts/validate_content.py --strict` 通过 | done |
| `REQ-0001-004` | `PRD-0001 §REQ-0001-004 [ECN-0002]` | `v1-gameplay-loop`、`v1-gameplay-rework`、`v1-input-hardening` | `GameSessionEngineTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest"` | `2026-04-01`：`GameSessionEngineTest` 已验证错字拒绝写入且不消耗候选池；`PlayLevelFlowTest` 已验证错字点击后格子仍为空且触发一次错误音；最新 `debug` 包已安装到设备 `QXNUT21B12002805` | done |
| `REQ-0001-005` | `PRD-0001 §REQ-0001-005 [ECN-0002]` | `v1-gameplay-loop`、`v1-gameplay-rework`、`v1-input-hardening` | `HintPolicyTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.HintPolicyTest"` | `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest` 全绿；`PlayLevelFlowTest` 已验证关卡页不存在 `释义提示` 卡片，释义仅作为朗读上下文保留 | done |
| `REQ-0001-006` | `PRD-0001 §REQ-0001-006 [ECN-0003]` | `v1-gameplay-loop`、`v1-gameplay-rework`、`v1-completion-speech-and-compact-layout` | `GameSpeechFormatterTest` | `PlayLevelFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"` | `2026-04-01`：`GameSpeechFormatterTest` 已覆盖答案播报格式；`PlayLevelFlowTest` 已验证手工填完、`提示一字` 补完、`揭示成语` 补完三条路径都会朗读一次成语答案，且释义重播仍不泄露答案 | done |
| `REQ-0001-007` | `PRD-0001 §REQ-0001-007 [ECN-0001]` | `v1-bootstrap-foundation`、`v1-gameplay-loop`、`v1-gameplay-rework` | `ProgressStoreTest` | `ContinueGameFlowTest` | `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest"` | `2026-04-01`：`ProgressStoreTest` 通过；`ContinueGameFlowTest` 已验证重启后恢复焦点格，`.\gradlew.bat :app:installDebug` 已把最新调试包装到设备 `QXNUT21B12002805` | done |
| `REQ-0001-008` | `PRD-0001 §REQ-0001-008 [ECN-0003]` | `v1-bootstrap-foundation`、`v1-completion-speech-and-compact-layout`、`v1-verification-content-pack` | `WordDragonTypographyTest`、`tools/content_pipeline/tests/test_layout_validator.py` | `PlayLevelFlowTest`、`RotateAndResumeTest` | `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --min-cell-sp 28 --min-touch-dp 56` | `2026-03-31`：`WordDragonTypographyTest` 通过；`2026-04-01`：`PlayLevelFlowTest` 已验证顶部显示 `第0001级`，且页面不再出现 `关卡练习`、`候选字盘` 与旧的 `chapter / level` 文案；布局校验脚本仍留在 `M4` | doing |
| `REQ-0001-009` | `PRD-0001 §REQ-0001-009` | `v1-content-pipeline`、`v1-verification-content-pack` | `tools/content_pipeline/tests/test_level_generator.py` | — | `uv run python tools/content_pipeline/scripts/build_levels.py --strict --min-levels 1000` | `2026-03-31`：`chapters.json` 统计 `20` 章、`1000` 关 | doing |
| `REQ-0001-010` | `PRD-0001 §REQ-0001-010 [ECN-0002]` | `v1-content-pipeline`、`v1-input-hardening` | `tools/content_pipeline/tests/test_validate_content.py` | — | `uv run python tools/content_pipeline/scripts/validate_content.py --strict` | `2026-03-31`：`research/content/content_provenance.md` 已记录来源、筛选与生成时间；`2026-04-01`：`test_validate_content.py` 已覆盖候选池频次不足用例，严格校验通过并可拦截字频覆盖不足 | done |
| `REQ-0001-011` | `PRD-0001 §REQ-0001-011` | `v1-gameplay-loop`、`v1-input-hardening`、`v1-verification-content-pack` | `GameSessionEngineTest`、`HintPolicyTest`、`ProgressStoreTest`、`GameSpeechFormatterTest`、`tools/content_pipeline/tests/test_level_generator.py`、`tools/content_pipeline/tests/test_validate_content.py` | `PlayLevelFlowTest`、`ContinueGameFlowTest`、`RotateAndResumeTest` | `.\gradlew.bat :app:testDebugUnitTest`；`.\gradlew.bat :app:connectedDebugAndroidTest`；`uv run python tools/content_pipeline/scripts/validate_content.py --strict` | `2026-04-01`：`.\gradlew.bat :app:testDebugUnitTest`、`PlayLevelFlowTest`、`ContinueGameFlowTest`、内容管线两组 pytest 与严格校验均通过；`RotateAndResumeTest` 仍留在 `M4` | doing |

## ECN 索引

- `docs/ecn/ECN-0001-word-dragon-gameplay-rework.md`：将关卡页交互改为棋盘驱动，并把 TTS 从“朗读答案”调整为“朗读线索/反馈”。
- `docs/ecn/ECN-0002-word-dragon-input-hardening.md`：移除释义卡片、改为错字拒绝写入，并修复候选池字频生成与校验规则。
- `docs/ecn/ECN-0003-word-dragon-completion-speech-and-compact-layout.md`：为成语首次完成补充答案播报，并继续压缩关卡页顶部与候选区。

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
- `M3` 已完成：运行时已接通离线资产读取、章节选关、候选字盘、提示、TTS、自动存档和继续游戏，两条核心仪测已在真机通过。
- `M3R` 已完成：`2026-04-01` 已通过 `GameSessionEngineTest`、`GameSpeechFormatterTest`、`ProgressStoreTest`、`PlayLevelFlowTest`、`ContinueGameFlowTest`，并已安装最新 `debug` 包到真机 `QXNUT21B12002805`。
- `M3H` 已完成：`2026-04-01` 已通过 `GameSessionEngineTest`、`PlayLevelFlowTest`、`ContinueGameFlowTest`、`tools/content_pipeline/tests/test_level_generator.py`、`tools/content_pipeline/tests/test_validate_content.py` 和 `uv run python tools/content_pipeline/scripts/validate_content.py --strict`；首关候选池已按真实频次重生成，最新 `debug` 包已安装到真机 `QXNUT21B12002805`。
- `M3C` 已完成：`2026-04-01` 已通过 `GameSpeechFormatterTest`、`PlayLevelFlowTest`、`ContinueGameFlowTest` 和 `.\gradlew.bat :app:testDebugUnitTest`；三种成语完成路径都会播报答案，顶部已改为 `第00xx级`，最新 `debug` 包已安装到真机 `QXNUT21B12002805`。
- 横竖屏自动化恢复验证和内容布局校验仍待 `v1-verification-content-pack` 实现。
