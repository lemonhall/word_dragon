# Word Dragon M5 Manual Idiom Curation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a manual review harness and frozen workspace for all `36084` idioms, enforce batch-by-batch human curation across `181` fixed batches, and assemble a traceable common-idiom dictionary for later content rebuild.

**Architecture:** Add a dedicated manual-review harness to the existing Python content pipeline. The harness reads the shipped catalog, writes immutable source batches into `research/content/manual_review/m5`, validates human-authored `review.jsonl` files one batch at a time, and derives `approved` outputs plus audit state. Human judgment stays in JSONL review files; Python only freezes input, verifies completeness and ordering, and assembles outputs.

**Tech Stack:** Python 3.13+, pytest, `uv`, JSON/JSONL, existing `word_dragon_content` package.

---

## PRD Trace

- `REQ-0001-002`
- `REQ-0001-010`

## Spec Trace

- `docs/superpowers/specs/2026-04-01-word-dragon-manual-idiom-curation-design.md`

## Scope

- 实现 `prepare`、`status`、`open-next`、`validate-batch`、`close-batch`、`assemble`
- 冻结 `idiom_catalog.json` 为唯一审核源，并切成 `181` 个固定批次
- 生成 `research/content/manual_review/m5/` 审核工作区
- 用自动化测试固化“不可跳批、不可漏审、不可重复、不可伪造源条目”
- 提供人工审核规则与执行留痕文档

## Out Of Scope

- 不在本计划中重建关卡或替换现有游戏内容包
- 不引入自动“判定常用/生僻”的算法
- 不增加 `keep/filter` 之外的第三种审核状态
- 不做 Android 客户端改动

## Acceptance

- `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/manual_review.py prepare --catalog app/src/main/assets/content/idiom_catalog.json --workspace research/content/manual_review/m5 --batch-size 200` 退出码为 `0`
- `uv run python tools/content_pipeline/scripts/manual_review.py status --workspace research/content/manual_review/m5 --json` 退出码为 `0`，并且 JSON 中 `total_entries=36084`、`total_batches=181`
- 当全部 `181` 批关闭后，`uv run python tools/content_pipeline/scripts/manual_review.py assemble --workspace research/content/manual_review/m5` 退出码为 `0`，并生成 `research/content/manual_review/m5/approved/final_common_idioms.json`
- 反作弊条款：如果脚本允许跳过前序批次直接关闭后续批次、允许手工伪造 `approved` 代替 `review`、允许漏条或重复条通过校验、或让算法直接替人工给出 `keep/filter` 结论，则本计划不得判为完成

## Files

- Create: `tools/content_pipeline/src/word_dragon_content/manual_review.py`
- Create: `tools/content_pipeline/scripts/manual_review.py`
- Create: `tools/content_pipeline/tests/test_manual_review.py`
- Create: `research/content/manual_review/m5/docs/m5-review-guideline.md`
- Create: `research/content/manual_review/m5/docs/m5-execution-notes.md`
- Create: `research/content/manual_review/m5/source/`
- Create: `research/content/manual_review/m5/reviews/`
- Create: `research/content/manual_review/m5/approved/`
- Create: `research/content/manual_review/m5/audit/`
- Modify: `docs/plan/v1-index.md`

### Task 1: Freeze the catalog into a deterministic review workspace

**Files:**
- Create: `tools/content_pipeline/tests/test_manual_review.py`
- Create: `tools/content_pipeline/src/word_dragon_content/manual_review.py`

- [ ] **Step 1: Write the failing prepare tests**

```python
def test_prepare_workspace_splits_enabled_entries_into_fixed_batches(tmp_path):
    report = prepare_workspace(catalog_path=catalog_path, workspace=tmp_path / "m5", batch_size=2)
    assert report["total_entries"] == 5
    assert report["total_batches"] == 3
    assert report["next_batch_id"] == "batch-0001"

def test_prepare_workspace_preserves_catalog_order_and_hashes(tmp_path):
    prepare_workspace(catalog_path=catalog_path, workspace=tmp_path / "m5", batch_size=200)
    manifest = json.loads((tmp_path / "m5" / "source" / "manifest.json").read_text(encoding="utf-8"))
    assert manifest["batches"][0]["start_global_seq"] == 1
    assert manifest["batches"][0]["end_global_seq"] == 2
```

- [ ] **Step 2: Run test to verify it fails**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k prepare_workspace`
Expected: FAIL because `prepare_workspace()` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```python
def prepare_workspace(catalog_path: Path, workspace: Path, batch_size: int) -> dict:
    payload = json.loads(catalog_path.read_text(encoding="utf-8"))
    entries = [entry for entry in payload["entries"] if entry.get("enabled")]
    total_entries = len(entries)
    total_batches = math.ceil(total_entries / batch_size)
    rows = [
        {
            "global_seq": index,
            "idiom_id": entry["id"],
            "text": entry["text"],
            "pinyin": entry.get("pinyin", ""),
            "short_explanation": entry.get("short_explanation", ""),
        }
        for index, entry in enumerate(entries, start=1)
    ]
    # slice rows by batch_size and write source/master_catalog.jsonl,
    # source/manifest.json, audit/progress.json and batch-0001..batch-0181 source files
    return {"total_entries": len(entries), "total_batches": total_batches, "next_batch_id": "batch-0001"}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k prepare_workspace`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/content_pipeline/tests/test_manual_review.py tools/content_pipeline/src/word_dragon_content/manual_review.py
git commit -m "v1: feat: add M5 workspace freezer"
```

### Task 2: Validate review files and reject malformed human decisions

**Files:**
- Modify: `tools/content_pipeline/tests/test_manual_review.py`
- Modify: `tools/content_pipeline/src/word_dragon_content/manual_review.py`

- [ ] **Step 1: Write the failing validation tests**

```python
def test_validate_review_batch_rejects_missing_rows_and_invalid_decisions(tmp_path):
    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")
    assert any("条目数不一致" in error for error in errors)
    assert any("decision" in error for error in errors)

def test_validate_review_batch_requires_reason_for_filter(tmp_path):
    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")
    assert any("过滤原因不能为空" in error for error in errors)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k validate_review_batch`
Expected: FAIL because `validate_review_batch()` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```python
def validate_review_batch(workspace: Path, batch_id: str) -> list[str]:
    source_rows = load_jsonl(workspace / "source" / "batches" / f"{batch_id}.source.jsonl")
    review_rows = load_jsonl(workspace / "reviews" / f"{batch_id}.review.jsonl")
    errors: list[str] = []
    if len(review_rows) != len(source_rows):
        errors.append(f"{batch_id} 条目数不一致。")
    for source_row, review_row in zip(source_rows, review_rows):
        if review_row["global_seq"] != source_row["global_seq"]:
            errors.append(f"{batch_id} 的 global_seq 不匹配。")
        if review_row["idiom_id"] != source_row["idiom_id"] or review_row["text"] != source_row["text"]:
            errors.append(f"{batch_id} 的词条标识不匹配。")
        if review_row["decision"] not in {"keep", "filter"}:
            errors.append(f"{batch_id} 的 decision 非法。")
        if review_row["decision"] == "filter" and not str(review_row.get("note", "")).strip():
            errors.append(f"{batch_id} 的过滤原因不能为空。")
    return errors
```

- [ ] **Step 4: Run test to verify it passes**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k validate_review_batch`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/content_pipeline/tests/test_manual_review.py tools/content_pipeline/src/word_dragon_content/manual_review.py
git commit -m "v1: feat: validate M5 review files"
```

### Task 3: Enforce sequential batch closing and final assembly

**Files:**
- Modify: `tools/content_pipeline/tests/test_manual_review.py`
- Modify: `tools/content_pipeline/src/word_dragon_content/manual_review.py`

- [ ] **Step 1: Write the failing close and assemble tests**

```python
def test_close_batch_blocks_skipped_predecessors(tmp_path):
    errors = close_batch(workspace=workspace, batch_id="batch-0002")
    assert any("前置批次" in error for error in errors)

def test_assemble_final_dictionary_requires_all_batches_closed(tmp_path):
    errors = assemble_final_dictionary(workspace=workspace)
    assert any("仍有未关闭批次" in error for error in errors)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k "close_batch or assemble_final_dictionary"`
Expected: FAIL because `close_batch()` and `assemble_final_dictionary()` do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```python
def read_status(workspace: Path) -> dict:
    return json.loads((workspace / "audit" / "progress.json").read_text(encoding="utf-8"))

def open_next_batch(workspace: Path) -> dict:
    progress = read_status(workspace)
    batch_id = progress["next_batch_id"]
    return {"batch_id": batch_id, "source_path": str(workspace / "source" / "batches" / f"{batch_id}.source.jsonl")}

def close_batch(workspace: Path, batch_id: str) -> list[str]:
    progress = read_status(workspace)
    previous_batch_id = f"batch-{int(batch_id.split('-')[1]) - 1:04d}"
    if batch_id != "batch-0001" and progress["batches"].get(previous_batch_id) != "closed":
        return [f"{batch_id} 的前置批次尚未关闭。"]
    errors = validate_review_batch(workspace=workspace, batch_id=batch_id)
    if errors:
        return errors
    review_rows = load_jsonl(workspace / "reviews" / f"{batch_id}.review.jsonl")
    keep_rows = [row for row in review_rows if row["decision"] == "keep"]
    write_jsonl(workspace / "approved" / f"{batch_id}.approved.jsonl", keep_rows)
    # update progress.json and batch report after approved output is derived
    return []

def assemble_final_dictionary(workspace: Path) -> list[str]:
    progress = read_status(workspace)
    if progress["closed_batches"] != progress["total_batches"]:
        return ["仍有未关闭批次，禁止汇总最终词典。"]
    rows = []
    for path in sorted((workspace / "approved").glob("batch-*.approved.jsonl")):
        rows.extend(load_jsonl(path))
    (workspace / "approved" / "final_common_idioms.json").write_text(
        json.dumps({"entry_count": len(rows), "entries": rows}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return []
```

- [ ] **Step 4: Run test to verify it passes**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k "close_batch or assemble_final_dictionary"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/content_pipeline/tests/test_manual_review.py tools/content_pipeline/src/word_dragon_content/manual_review.py
git commit -m "v1: feat: enforce M5 batch order and assembly"
```

### Task 4: Expose the harness as a CLI and write the reviewer guide

**Files:**
- Create: `tools/content_pipeline/scripts/manual_review.py`
- Modify: `tools/content_pipeline/tests/test_manual_review.py`
- Create: `research/content/manual_review/m5/docs/m5-review-guideline.md`
- Create: `research/content/manual_review/m5/docs/m5-execution-notes.md`

- [ ] **Step 1: Write the failing CLI tests**

```python
def test_manual_review_cli_status_outputs_json(tmp_path, capsys):
    exit_code = main(["status", "--workspace", str(workspace), "--json"])
    payload = json.loads(capsys.readouterr().out)
    assert exit_code == 0
    assert payload["total_entries"] == 5
    assert payload["next_batch_id"] == "batch-0001"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q -k manual_review_cli`
Expected: FAIL because `tools/content_pipeline/scripts/manual_review.py` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```python
def main(argv: list[str] | None = None) -> int:
    # expose exactly six subcommands:
    # prepare, status, open-next, validate-batch, close-batch, assemble
    # and print JSON so `status --json` can be asserted in tests and reports
    return 0
```

```markdown
# M5 审核规则

- 只能人工决定 `keep` 或 `filter`
- `filter` 必须写明原因
- 不允许脚本批量改写 `review.jsonl`
- 先运行 `open-next`，只处理返回的那一批
- 每批完成后必须先 `validate-batch`，再 `close-batch`
```

- [ ] **Step 4: Run test to verify it passes**

Run: `uv run pytest tools/content_pipeline/tests/test_manual_review.py -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/content_pipeline/scripts/manual_review.py tools/content_pipeline/tests/test_manual_review.py tools/content_pipeline/src/word_dragon_content/manual_review.py research/content/manual_review/m5/docs/m5-review-guideline.md research/content/manual_review/m5/docs/m5-execution-notes.md
git commit -m "v1: feat: add M5 manual review CLI"
```

### Task 5: Bootstrap the real workspace and run the human review loop

**Files:**
- Create: `research/content/manual_review/m5/source/`
- Create: `research/content/manual_review/m5/reviews/`
- Create: `research/content/manual_review/m5/approved/`
- Create: `research/content/manual_review/m5/audit/`
- Modify: `docs/plan/v1-index.md`

- [ ] **Step 1: Run `prepare` against the real catalog**

Run: `uv run python tools/content_pipeline/scripts/manual_review.py prepare --catalog app/src/main/assets/content/idiom_catalog.json --workspace research/content/manual_review/m5 --batch-size 200`
Expected: exit `0`, printed JSON contains `total_entries=36084` and `total_batches=181`.

- [ ] **Step 2: Run `status --json` and verify frozen totals**

Run: `uv run python tools/content_pipeline/scripts/manual_review.py status --workspace research/content/manual_review/m5 --json`
Expected: exit `0`, JSON contains `"total_entries": 36084`, `"total_batches": 181`, `"closed_batches": 0`, `"next_batch_id": "batch-0001"`.

- [ ] **Step 3: Execute the closed-loop batch procedure**

```json
{"global_seq":1,"idiom_id":"idiom-00001","text":"一世两清","decision":"filter","note":"偏冷僻，现代大众语感极弱，不适合作为常用成语游戏词条。"}
{"global_seq":2,"idiom_id":"idiom-00002","text":"一世之利","decision":"filter","note":"古书语感明显，普通玩家陌生。"}
{"global_seq":3,"idiom_id":"idiom-00003","text":"一世龙门","decision":"filter","note":"过于典故化，不适合作为常用成语词条。"}
```

Run in order:
- `uv run python tools/content_pipeline/scripts/manual_review.py open-next --workspace research/content/manual_review/m5`
- 人工逐条填写对应的 `reviews/batch-xxxx.review.jsonl`
- `uv run python tools/content_pipeline/scripts/manual_review.py validate-batch --workspace research/content/manual_review/m5 --batch-id batch-xxxx`
- `uv run python tools/content_pipeline/scripts/manual_review.py close-batch --workspace research/content/manual_review/m5 --batch-id batch-xxxx`

Expected: 只能按顺序推进；每批关闭后生成对应 `approved` 和 `batch_reports`；直到 `status --json` 显示 `closed_batches=181`、`next_batch_id=null`。

- [ ] **Step 4: Assemble the final common-idiom dictionary**

Run: `uv run python tools/content_pipeline/scripts/manual_review.py assemble --workspace research/content/manual_review/m5`
Expected: exit `0`, 生成 `research/content/manual_review/m5/approved/final_common_idioms.json`。

- [ ] **Step 5: Commit**

```bash
git add research/content/manual_review/m5 docs/plan/v1-index.md
git commit -m "v1: data: curate common idiom dictionary"
```

## Risks

- 最大风险是执行周期，而不是编码复杂度；如果不把每一批都当成强状态机闭环，人工审核必然失控。
- 如果 `prepare` 后又改动上游 `idiom_catalog.json` 却不重建工作区，整个追溯链会失真，因此冻结源后必须锁定版本。
- 如果把 `approved` 允许为人工输入文件，后续无法证明“保留词条一定来自真实 review 记录”，因此必须保持派生关系。
- Task 5 的审核环节是长周期人工执行，不允许用批处理脚本绕过逐条人工判断。
