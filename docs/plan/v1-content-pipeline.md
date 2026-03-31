# v1 Content Pipeline

## Goal

建立成语词条清洗、常用度筛选、固定关卡生成、章节索引输出和内容校验链路，产出 `v1` 可用的内置内容包。

## PRD Trace

- `REQ-0001-002`
- `REQ-0001-003`
- `REQ-0001-009`
- `REQ-0001-010`

## Scope

- 使用 `uv` 和 Python 建立本地内容构建工具链
- 读取原始成语数据与常用度参考数据
- 筛选四字常用成语并统一短释义字段
- 批量生成固定关卡与章节索引
- 输出内容校验报告，确保 `>= 1000` 关且无断链

## Out Of Scope

- 不在客户端实现动态关卡生成
- 不实现线上内容更新
- 不做关卡页 UI

## Acceptance

- `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q` 退出码为 `0`
- `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q` 退出码为 `0`
- `uv run pytest tools/content_pipeline/tests/test_validate_content.py -q` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/build_catalog.py --strict --output app/src/main/assets/content/idiom_catalog.json` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/build_levels.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/validate_content.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels` 退出码为 `0`
- 反作弊条款：生成结果中任何 `enabled=true` 的词条如果不是四字成语、任一关卡如果含超过 `8` 个成语、或章节索引统计关卡数低于 `1000`，都必须让校验脚本返回非零退出码

## Files

- Create: `tools/content_pipeline/pyproject.toml`
- Create: `tools/content_pipeline/src/word_dragon_content/__init__.py`
- Create: `tools/content_pipeline/src/word_dragon_content/catalog_filter.py`
- Create: `tools/content_pipeline/src/word_dragon_content/level_generator.py`
- Create: `tools/content_pipeline/src/word_dragon_content/content_validator.py`
- Create: `tools/content_pipeline/src/word_dragon_content/source_manifest.py`
- Create: `tools/content_pipeline/scripts/build_catalog.py`
- Create: `tools/content_pipeline/scripts/build_levels.py`
- Create: `tools/content_pipeline/scripts/validate_content.py`
- Create: `tools/content_pipeline/tests/test_catalog_filter.py`
- Create: `tools/content_pipeline/tests/test_level_generator.py`
- Create: `tools/content_pipeline/tests/test_validate_content.py`
- Create: `research/content/content_provenance.md`
- Create: `app/src/main/assets/content/idiom_catalog.json`
- Create: `app/src/main/assets/content/chapters.json`
- Create: `app/src/main/assets/content/levels/`

## Steps

1. 先写 `test_catalog_filter.py`、`test_level_generator.py`、`test_validate_content.py` 三组失败测试，覆盖四字过滤、关卡数量上限、连通性、重复关卡和索引断链。
2. 运行 `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q`，预期失败原因是内容管线包和测试目标尚不存在。
3. 实现 `catalog_filter.py` 和 `source_manifest.py`，把原始来源清洗为统一词条格式，并输出 `idiom_catalog.json`。
4. 再次运行 `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q`，预期通过。
5. 运行 `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q`，预期失败原因是关卡生成逻辑尚未实现。
6. 实现 `level_generator.py`、`build_levels.py` 和章节索引输出，确保单关成语数 `4-8`，总关卡数至少 `1000`。
7. 再次运行 `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q`，预期通过。
8. 运行 `uv run pytest tools/content_pipeline/tests/test_validate_content.py -q`，预期失败原因是完整校验逻辑尚未实现。
9. 实现 `content_validator.py` 与 `validate_content.py`，把四字规则、连通性、重复关卡、关卡总数和章节断链检查全部固化。
10. 再次运行 `uv run pytest tools/content_pipeline/tests/test_validate_content.py -q`，预期通过。
11. 运行 `uv run python tools/content_pipeline/scripts/build_catalog.py --strict --output app/src/main/assets/content/idiom_catalog.json`，预期生成词条数据文件。
12. 运行 `uv run python tools/content_pipeline/scripts/build_levels.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8`，预期生成章节与关卡包。
13. 运行 `uv run python tools/content_pipeline/scripts/validate_content.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels`，预期输出通过并返回 `0`。
14. 在 `research/content/content_provenance.md` 记录词库来源、常用度排序来源、短释义改写策略和生成时间戳。

## Risks

- 原始来源中的释义风格可能过长，需要在清洗阶段统一裁剪成适合 UI 的短句。
- 自动拼盘容易生成视觉质量不佳的关卡，因此校验脚本必须尽早覆盖连通性与密度上限。
- 若原始数据授权口径变化，需要通过 ECN 更新内容来源策略。
