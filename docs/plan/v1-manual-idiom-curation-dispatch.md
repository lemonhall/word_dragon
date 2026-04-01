# M5 Dispatch Manifest

## Purpose

这份文档只回答一件事：在 `M5` 正式开始实现前，先把可并行的子任务切开，并在本地确认写集不冲突、顺序依赖明确、切分产物已校验通过。只有这份清单校验为 `pass`，才允许开始子代理分发。

## Pre-Dispatch Gate

- 任务切分清单必须已经写明每个 Lane 的写集与责任边界
- Lane A 与 Lane B 的写集必须不重叠
- `research/content/manual_review/m5/source/`、`approved/`、`audit/` 这些派生产物目录，不允许在首轮并行阶段分配给子代理直接写入
- 并发上限必须明确写为 `<= 5`

## Post-Merge Gate

- 只有 Lane A/B 合并并通过本地测试后，主线程才允许执行 `prepare`
- `prepare` 成功后，必须验证 `181` 批源文件、`status --json` 结果和 `source_hashes.json`
- 在真实词库切分校验完成之前，不允许进入任何人工审核批次执行

## Slice Plan

### Lane A

- Owner: Worker A
- Write set:
  - `tools/content_pipeline/src/word_dragon_content/manual_review.py`
  - `tools/content_pipeline/tests/test_manual_review.py`
- Responsibility:
  - 实现核心状态机、冻结源、review 校验、顺序关闭、最终汇总
  - 把 `source_hashes`、`pending/in_review/validated/closed` 状态流转固化进测试

### Lane B

- Owner: Worker B
- Write set:
  - `tools/content_pipeline/scripts/manual_review.py`
  - `research/content/manual_review/m5/docs/m5-review-guideline.md`
  - `research/content/manual_review/m5/docs/m5-execution-notes.md`
- Responsibility:
  - 实现 CLI 命令面
  - 补全人工审核操作说明
  - 不直接写 `approved/` 或 `audit/` 产物

### Lane C

- Owner: Main rollout only
- Write set:
  - `research/content/manual_review/m5/source/`
  - `research/content/manual_review/m5/audit/`
  - `research/content/manual_review/m5/approved/`
  - `docs/plan/v1-index.md`
- Responsibility:
  - 在 Lane A/B 合并后，本地运行 `prepare`
  - 校验切分产物
  - 再决定后续人工审核批次如何推进

## Conflict Check

- Lane A 与 Lane B 的写集不重叠
- Lane C 在首轮并行阶段不分发给子代理，避免和 A/B 抢写工作区产物
- `approved/`、`audit/`、`source/` 这些派生产物目录不授予任何首轮子代理直接写权限
- 首轮并发数固定为 `2`，满足“并发控制在 `5` 以内”

## Validation Result

- Task-slice check: `pass`
- Write-set disjoint check: `pass`
- Dependency order check: `pass`
- Dispatch readiness: `pass`

## Dispatch Order

1. Worker A 先实现核心库与测试
2. Worker B 并行实现 CLI 和审核说明文档
3. 主线程合并 A/B 结果并本地跑测试
4. 主线程执行 `prepare` 和切分产物校验
5. 只有上述步骤都通过后，才进入后续批次人工审核执行
