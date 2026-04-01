# v1 Curated 1500-Level Content Pack

## Goal

基于 `M5` 的人工审核结果，收敛出最终常用成语词库，重建 `1500` 关离线内容包，并证明“人工审核 keep 数 = 最终词库数 = 关卡唯一成语数”。

## PRD Trace

- `REQ-0001-002`
- `REQ-0001-009`
- `REQ-0001-010`
- `REQ-0001-011`

## Scope

- 从 `research/content/manual_review/m5/reviews/` 汇总所有 `keep`
- 生成 `research/content/manual_review/m5/approved/final_common_idioms.json`
- 用最终常用词库重建 `1500` 关固定关卡
- 允许成语在不同关卡复用，但必须保证所有 `keep` 成语至少出现一次
- 将 Android 运行时内容路径切到 `app/src/main/assets/content/m6/`
- 完成内容校验、布局校验、真机安装验证

## Out Of Scope

- 不在本计划中继续做自动筛词算法
- 不做联网更新词库或在线内容分发
- 不清理旧的 `content/levels` 历史资产目录

## Acceptance

- `python tools/content_pipeline/scripts/build_curated_catalog.py --catalog app/src/main/assets/content/idiom_catalog.json --workspace research/content/manual_review/m5 --output research/content/manual_review/m5/approved/final_common_idioms.json` 退出码为 `0`，且输出 `entry_count=10251`
- `python tools/content_pipeline/scripts/build_levels.py --catalog app/src/main/assets/content/m6/idiom_catalog.json --output-dir app/src/main/assets/content/m6/levels --chapter-index app/src/main/assets/content/m6/chapters.json --min-levels 1500 --max-idioms-per-level 8 --preferred-idioms-per-level 8 --require-full-catalog-coverage --max-board-width 9 --strict` 退出码为 `0`
- `python tools/content_pipeline/scripts/validate_content.py --strict --catalog app/src/main/assets/content/m6/idiom_catalog.json --chapter-index app/src/main/assets/content/m6/chapters.json --levels-dir app/src/main/assets/content/m6/levels` 退出码为 `0`
- `python tools/content_pipeline/scripts/verify_curated_pack.py --workspace research/content/manual_review/m5 --catalog app/src/main/assets/content/m6/idiom_catalog.json --chapter-index app/src/main/assets/content/m6/chapters.json --levels-dir app/src/main/assets/content/m6/levels --expected-levels 1500` 退出码为 `0`，并输出：
  - `keep_count=10251`
  - `catalog_entry_count=10251`
  - `used_unique_idiom_count=10251`
  - `total_levels=1500`
- `python tools/content_pipeline/scripts/validate_layouts.py --strict --catalog app/src/main/assets/content/m6/idiom_catalog.json --chapter-index app/src/main/assets/content/m6/chapters.json --levels-dir app/src/main/assets/content/m6/levels --min-cell-sp 28 --min-touch-dp 56` 退出码为 `0`
- `.\gradlew.bat :app:assembleDebug` 与 `.\gradlew.bat :app:installDebug` 退出码为 `0`
- 反作弊条款：如果最终关卡没有覆盖全部 `keep` 成语、唯一成语数和人工审核结果对不上、或只改文案不真正切到新内容包，则本计划不得判为完成

## Files

- Create: `tools/content_pipeline/src/word_dragon_content/curated_pack.py`
- Create: `tools/content_pipeline/scripts/build_curated_catalog.py`
- Create: `tools/content_pipeline/scripts/verify_curated_pack.py`
- Create: `tools/content_pipeline/tests/test_curated_pack.py`
- Create: `app/src/main/assets/content/m6/idiom_catalog.json`
- Create: `app/src/main/assets/content/m6/chapters.json`
- Create: `app/src/main/assets/content/m6/levels/`
- Modify: `tools/content_pipeline/src/word_dragon_content/level_generator.py`
- Modify: `tools/content_pipeline/scripts/build_levels.py`
- Modify: `tools/content_pipeline/scripts/validate_content.py`
- Modify: `tools/content_pipeline/tests/test_level_generator.py`
- Modify: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetIdiomCatalogDataSource.kt`
- Modify: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetLevelPackDataSource.kt`
- Modify: `docs/plan/v1-index.md`

## Verification Evidence

- `2026-04-01`：`build_curated_catalog.py` 产出 `final_common_idioms.json`，`entry_count=10251`，并兼容 `29` 个旧 `review` 字段批次
- `2026-04-01`：`build_levels.py` 基于最终词库生成 `1500` 关，运行时内容落在 `app/src/main/assets/content/m6/`
- `2026-04-01`：`verify_curated_pack.py` 输出 `keep_count=10251`、`catalog_entry_count=10251`、`used_unique_idiom_count=10251`、`total_level_slots=12000`、`total_levels=1500`
- `2026-04-01`：`validate_layouts.py --strict` 输出 `max_board_width=9`、`smallest_board_cell_dp=36`、`horizontal_scroll_levels=0`
- `2026-04-01`：`.\gradlew.bat :app:assembleDebug` 成功；`.\gradlew.bat :app:installDebug` 已安装到设备 `QXNUT21B12002805`

## Risks

- 当前保留了旧的 `content/` 历史资产，APK 会比只保留 `m6` 内容略大，但运行时已明确切到 `content/m6/`
- `portrait_scroll_levels=940` 说明部分关卡仍需要纵向滚动，但未触发横向滚动或最小字号违规
