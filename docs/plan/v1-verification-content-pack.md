# v1 Verification Content Pack

## Goal

完成 `v1` 的内容资源验收、横竖屏适配验证、继续游戏 E2E 和交付前检查，使 `v1` 具备可发布的证据链。

## PRD Trace

- `REQ-0001-008`
- `REQ-0001-009`
- `REQ-0001-010`
- `REQ-0001-011`

## Scope

- 建立布局校验脚本与阈值
- 运行横竖屏和继续游戏的仪器测试
- 验证内容资源数量、索引完整性和断网启动
- 汇总发布前证据

## Out Of Scope

- 不在此计划中新增玩法需求
- 不做排行榜、账号或网络功能扩展
- 不做商店上架材料

## Acceptance

- `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels --min-cell-sp 28 --min-touch-dp 56` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest,me.lemonhall.worddragon.e2e.RotateAndResumeTest` 退出码为 `0`
- `.\gradlew.bat :app:assembleDebug` 退出码为 `0`
- 内容统计报告显示总关卡数 `>= 1000`
- 反作弊条款：如果布局校验被跳过、继续游戏只在同一进程内有效、或通过硬编码假数据掩盖真实内容数量，则本计划不得判为完成

## Files

- Create: `tools/content_pipeline/src/word_dragon_content/layout_validator.py`
- Create: `tools/content_pipeline/scripts/validate_layouts.py`
- Create: `tools/content_pipeline/tests/test_layout_validator.py`
- Create: `app/src/androidTest/java/me/lemonhall/worddragon/e2e/RotateAndResumeTest.kt`
- Create: `docs/release/v1-verification-report.md`

## Steps

1. 编写 `test_layout_validator.py` 和 `RotateAndResumeTest.kt` 两个失败测试，覆盖最小字号阈值、最小触控尺寸阈值和旋转恢复关卡状态。
2. 运行 `uv run pytest tools/content_pipeline/tests/test_layout_validator.py -q`，预期失败原因是布局校验器尚未实现。
3. 实现 `layout_validator.py` 和 `validate_layouts.py`，将横屏、竖屏、字号和触控尺寸规则固化为脚本。
4. 再次运行 `uv run pytest tools/content_pipeline/tests/test_layout_validator.py -q`，预期通过。
5. 运行 `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.RotateAndResumeTest`，预期失败原因是旋转恢复链路尚未全部达标。
6. 修正或补全旋转恢复、继续游戏和状态持久化实现，直到 `RotateAndResumeTest` 与 `ContinueGameFlowTest` 一起通过。
7. 运行 `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels --min-cell-sp 28 --min-touch-dp 56`，预期输出通过。
8. 运行 `.\gradlew.bat :app:assembleDebug`，预期成功构建。
9. 在 `docs/release/v1-verification-report.md` 汇总验证命令、通过时间、内容数量和已知剩余风险。

## Risks

- Android 仪器测试对旋转恢复较敏感，若状态提升链路不清晰，容易出现间歇失败。
- 布局阈值若设置过高，可能压缩可用关卡数；若设置过低，会直接损害老年用户体验。
