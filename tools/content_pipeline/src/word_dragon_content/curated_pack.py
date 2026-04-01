from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from word_dragon_content.manual_review import load_jsonl


def normalize_review_decision(row: dict[str, object]) -> str:
    decision = row.get("decision", row.get("review"))
    if decision not in {"keep", "filter"}:
        raise ValueError(f"Unsupported review decision: {decision!r}")
    return str(decision)


def summarize_review_workspace(workspace: Path) -> dict[str, object]:
    workspace = Path(workspace)
    keep_ids_in_order: list[str] = []
    keep_id_set: set[str] = set()
    total_rows = 0
    keep_count = 0
    filter_count = 0
    legacy_schema_files = 0

    for path in sorted((workspace / "reviews").glob("batch-*.review.jsonl")):
        rows = load_jsonl(path)
        file_uses_legacy_schema = False
        for row in rows:
            total_rows += 1
            decision = normalize_review_decision(row)
            if "review" in row and "decision" not in row:
                file_uses_legacy_schema = True
            idiom_id = row.get("idiom_id")
            if not isinstance(idiom_id, str) or not idiom_id:
                raise ValueError(f"{path} contains a row without a valid idiom_id")
            if decision == "keep":
                keep_count += 1
                keep_ids_in_order.append(idiom_id)
                if idiom_id in keep_id_set:
                    raise ValueError(f"Duplicate kept idiom_id found in reviews: {idiom_id}")
                keep_id_set.add(idiom_id)
            else:
                filter_count += 1
        if file_uses_legacy_schema:
            legacy_schema_files += 1

    return {
        "total_rows": total_rows,
        "keep_count": keep_count,
        "filter_count": filter_count,
        "keep_ids_in_order": keep_ids_in_order,
        "keep_id_set": keep_id_set,
        "legacy_schema_files": legacy_schema_files,
    }


def build_curated_catalog_payload(catalog_path: Path, workspace: Path) -> dict[str, object]:
    catalog_path = Path(catalog_path)
    workspace = Path(workspace)
    source_payload = json.loads(catalog_path.read_text(encoding="utf-8"))
    if isinstance(source_payload, dict):
        source_entries = source_payload.get("entries", [])
        source_manifest = source_payload.get("manifest")
        source_generated_at = source_payload.get("generated_at")
    else:
        source_entries = source_payload
        source_manifest = None
        source_generated_at = None

    if not isinstance(source_entries, list):
        raise ValueError("Catalog entries must be a list")

    review_summary = summarize_review_workspace(workspace)
    keep_id_set: set[str] = review_summary["keep_id_set"]  # type: ignore[assignment]

    curated_entries: list[dict[str, object]] = []
    seen_ids: set[str] = set()
    for entry in source_entries:
        if not isinstance(entry, dict):
            raise ValueError("Catalog entry must be an object")
        idiom_id = entry.get("id")
        if not isinstance(idiom_id, str) or not idiom_id:
            raise ValueError("Catalog entry missing valid id")
        if idiom_id not in keep_id_set:
            continue
        if not entry.get("enabled", True):
            raise ValueError(f"Kept idiom {idiom_id} is not enabled in source catalog")
        curated_entries.append(dict(entry))
        seen_ids.add(idiom_id)

    missing_ids = sorted(keep_id_set - seen_ids)
    if missing_ids:
        raise ValueError(f"Review workspace references missing catalog entries: {missing_ids[:10]}")

    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "manifest": {
            "source_catalog_path": str(catalog_path.resolve()),
            "source_generated_at": source_generated_at,
            "source_manifest": source_manifest,
            "manual_review": {
                "workspace": str(workspace.resolve()),
                "total_rows": review_summary["total_rows"],
                "keep_count": review_summary["keep_count"],
                "filter_count": review_summary["filter_count"],
                "legacy_schema_files": review_summary["legacy_schema_files"],
                "accepted_decision_fields": ["decision", "review"],
            },
        },
        "entry_count": len(curated_entries),
        "entries": curated_entries,
    }


def summarize_level_usage(levels_dir: Path) -> dict[str, object]:
    levels_dir = Path(levels_dir)
    unique_idioms: set[str] = set()
    total_slots = 0
    total_levels = 0

    for path in sorted(levels_dir.glob("level-*.json")):
        level = json.loads(path.read_text(encoding="utf-8"))
        idiom_ids = level.get("idiom_ids", [])
        if not isinstance(idiom_ids, list):
            raise ValueError(f"{path} has invalid idiom_ids")
        total_levels += 1
        total_slots += len(idiom_ids)
        for idiom_id in idiom_ids:
            if not isinstance(idiom_id, str) or not idiom_id:
                raise ValueError(f"{path} contains invalid idiom_id")
            unique_idioms.add(idiom_id)

    return {
        "total_levels": total_levels,
        "total_slots": total_slots,
        "unique_idiom_count": len(unique_idioms),
        "unique_idioms": unique_idioms,
    }


def validate_curated_pack(
    *,
    workspace: Path,
    catalog_path: Path,
    chapter_index_path: Path,
    levels_dir: Path,
    expected_levels: int,
) -> tuple[list[str], dict[str, object]]:
    workspace = Path(workspace)
    catalog_path = Path(catalog_path)
    chapter_index_path = Path(chapter_index_path)
    levels_dir = Path(levels_dir)

    review_summary = summarize_review_workspace(workspace)
    catalog_payload = json.loads(catalog_path.read_text(encoding="utf-8"))
    chapter_index = json.loads(chapter_index_path.read_text(encoding="utf-8"))
    level_usage = summarize_level_usage(levels_dir)

    catalog_entries = catalog_payload["entries"] if isinstance(catalog_payload, dict) else catalog_payload
    catalog_ids = {entry["id"] for entry in catalog_entries}
    keep_ids: set[str] = review_summary["keep_id_set"]  # type: ignore[assignment]
    used_ids: set[str] = level_usage["unique_idioms"]  # type: ignore[assignment]

    errors: list[str] = []
    if review_summary["keep_count"] != len(catalog_entries):
        errors.append(
            f"人工审核 keep 数量 {review_summary['keep_count']} 与最终词库数量 {len(catalog_entries)} 不一致。"
        )
    if review_summary["keep_count"] != level_usage["unique_idiom_count"]:
        errors.append(
            f"人工审核 keep 数量 {review_summary['keep_count']} 与关卡中唯一成语数量 "
            f"{level_usage['unique_idiom_count']} 不一致。"
        )
    if keep_ids != catalog_ids:
        errors.append("最终词库与人工审核 keep 集合不一致。")
    if used_ids != catalog_ids:
        errors.append("关卡实际使用的唯一成语集合与最终词库不一致。")
    if level_usage["total_levels"] != expected_levels:
        errors.append(
            f"关卡文件数量 {level_usage['total_levels']} 与预期 {expected_levels} 不一致。"
        )
    if chapter_index.get("total_levels") != expected_levels:
        errors.append(
            f"章节索引 total_levels={chapter_index.get('total_levels')} 与预期 {expected_levels} 不一致。"
        )

    report = {
        "keep_count": review_summary["keep_count"],
        "filter_count": review_summary["filter_count"],
        "catalog_entry_count": len(catalog_entries),
        "used_unique_idiom_count": level_usage["unique_idiom_count"],
        "total_level_slots": level_usage["total_slots"],
        "total_levels": level_usage["total_levels"],
        "chapter_total_levels": chapter_index.get("total_levels"),
        "legacy_schema_files": review_summary["legacy_schema_files"],
    }
    return errors, report
