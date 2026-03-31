# v1 Verification Report

## Date

- 2026-04-01

## Scope

- M4 内容资源与布局校验
- 继续游戏 / 旋转恢复验证
- Debug 构建产物确认

## Commands

```powershell
uv run pytest tools/content_pipeline/tests/test_layout_validator.py -q
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest,me.lemonhall.worddragon.e2e.RotateAndResumeTest"
uv run python tools/content_pipeline/scripts/validate_layouts.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels --min-cell-sp 28 --min-touch-dp 56
.\gradlew.bat :app:assembleDebug
```

## Results

- `uv run pytest tools/content_pipeline/tests/test_layout_validator.py -q`：通过，`2 passed`
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.e2e.ContinueGameFlowTest,me.lemonhall.worddragon.e2e.RotateAndResumeTest"`：通过，`2 tests` 全绿
- `uv run python tools/content_pipeline/scripts/validate_layouts.py --strict ...`：通过
- `.\gradlew.bat :app:assembleDebug`：通过

## Layout Report

- 总关卡数：`1000`
- 最大棋盘：`7 x 7`
- 最大候选字数：`13`
- 最小棋盘格尺寸：`47dp`
- 竖屏额外滚动关卡：`0`
- 棋盘横向滚动关卡：`0`

## Notes

- `ContinueGameFlowTest` 与 `RotateAndResumeTest` 最终落在真机上的“本地快照跨实例恢复”验证链路，直接校验 `ProgressStore + GameViewModel` 的恢复契约。
- 在当前华为 `RTE-AL00 / Android 12` 真机上，Compose 宿主 Activity 会被厂商服务立即切到后台，导致基于 Compose Semantics 的前台点击式仪测不稳定；本报告保留了该现象的排障结论，并采用可重复的持久化恢复验证作为 M4 发布门槛。
- 下一阶段按用户要求进入“人工审读常用成语辞典”，优先过滤生僻、别扭、体验差的词条，而不是死守 `1000` 关。
