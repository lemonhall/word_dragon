# Word Dragon 内容来源留痕

## 本次生成

- 生成时间：2026-03-31 21:55 CST
- 生成命令：
  - `uv run python tools/content_pipeline/scripts/build_catalog.py --strict --output app/src/main/assets/content/idiom_catalog.json`
  - `uv run python tools/content_pipeline/scripts/build_levels.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8`
  - `uv run python tools/content_pipeline/scripts/validate_content.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels`

## 上游来源

### 词典与释义素材

- 来源：`mapull/chinese-dictionary`
- 仓库：<https://github.com/mapull/chinese-dictionary>
- 本地原始副本：
  - `tools/content_pipeline/data/raw/mapull/idiom.json`
  - `tools/content_pipeline/data/raw/mapull/char_common_base.json`
- 上游许可：MIT
- 备注：上游 README 明确说明其汇总数据中，部分更早来源可能无法完全确认，因此 `word_dragon` 不直接原样分发全部原始字段，而是只使用四字成语、常用字频和清洗后的短释义。

### 上游参考链

- `mapull/chinese-dictionary` README 明确列出其参考过 `pwxcoo/chinese-xinhua` 等开源资料。
- `word_dragon` 当前不直接抓取或运行时访问这些上游站点，构建仅依赖仓库内保存的原始副本。

## 本地筛选策略

### 成语启用规则

- 只保留四个汉字的成语，带逗号、顿号、阿拉伯数字或非汉字字符的条目直接剔除。
- 严格模式下，每个字都必须能在 `char_common_base.json` 中找到，且单字频级不得高于 `2`。
- 严格模式下，四字频级求和 `frequency_rank` 不得高于 `5`。
- 生成后的可玩词条统一标记 `enabled=true`。

### 短释义改写

- 取原释义的第一句或第一分句。
- 去掉多余空白，统一补成短句句号。
- 控制在适合大字号 UI 的短句长度内。
- TTS 文案统一为 `成语。短释义。`

### 难度分层

- `starter`：`frequency_rank <= 3`
- `standard`：`frequency_rank <= 5`
- `advanced`：其余保留下来的条目

## 关卡生成策略

- 当前 `v1` 内容包采用 `chain-4` 连通模板。
- 每关固定 `4` 个成语，满足 PRD 中 `4-8` 的上限要求，并优先让大字号布局更稳。
- 每关通过共享汉字形成单链连通，不存在孤立词条。
- 当前生成统计：
  - 可玩词条：`36084`
  - 章节数：`20`
  - 关卡数：`1000`
  - 单关成语数：最少 `4`，最多 `4`
  - 最大棋盘尺寸：`7 x 7`

## 自动化验证

- `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py -q`
- `uv run pytest tools/content_pipeline/tests/test_level_generator.py -q`
- `uv run pytest tools/content_pipeline/tests/test_validate_content.py -q`
- `uv run python tools/content_pipeline/scripts/validate_content.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --chapter-index app/src/main/assets/content/chapters.json --levels-dir app/src/main/assets/content/levels`

## 后续注意事项

- 如果后续要引入更密集的 `5-8` 成语模板，必须同步扩展布局校验脚本，确保老年大字号基线不被破坏。
- 如果词典来源或启用阈值变化，必须同步更新本文件与 `source_manifest.py`。
