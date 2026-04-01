import json
from pathlib import Path

from word_dragon_content.curated_pack import (
    build_curated_catalog_payload,
    validate_curated_pack,
)


def _write_catalog(tmp_path: Path) -> Path:
    payload = {
        "generated_at": "2026-04-01T00:00:00Z",
        "manifest": {"source": "unit-test"},
        "entries": [
            {"id": "idiom-00001", "text": "一心一意", "pinyin": "", "short_explanation": "专心。", "enabled": True},
            {"id": "idiom-00002", "text": "一团和气", "pinyin": "", "short_explanation": "和气。", "enabled": True},
            {"id": "idiom-00003", "text": "一马当先", "pinyin": "", "short_explanation": "领先。", "enabled": True},
            {"id": "idiom-00004", "text": "一举两得", "pinyin": "", "short_explanation": "两得。", "enabled": True},
            {"id": "idiom-00005", "text": "一知半解", "pinyin": "", "short_explanation": "知道不多。", "enabled": True},
        ],
    }
    path = tmp_path / "idiom_catalog.json"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def _write_review_file(workspace: Path, batch_id: str, rows: list[dict]) -> None:
    review_path = workspace / "reviews" / f"{batch_id}.review.jsonl"
    review_path.parent.mkdir(parents=True, exist_ok=True)
    with review_path.open("w", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False))
            handle.write("\n")


def test_build_curated_catalog_payload_supports_legacy_review_schema(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"
    _write_review_file(
        workspace,
        "batch-0001",
        [
            {"global_seq": 1, "idiom_id": "idiom-00001", "text": "一心一意", "decision": "keep", "note": ""},
            {"global_seq": 2, "idiom_id": "idiom-00002", "text": "一团和气", "review": "filter", "note": "偏口语"},
            {"global_seq": 3, "idiom_id": "idiom-00003", "text": "一马当先", "review": "keep", "note": ""},
            {"global_seq": 4, "idiom_id": "idiom-00004", "text": "一举两得", "decision": "filter", "note": "略书面"},
            {"global_seq": 5, "idiom_id": "idiom-00005", "text": "一知半解", "review": "keep", "note": ""},
        ],
    )

    payload = build_curated_catalog_payload(catalog_path=catalog_path, workspace=workspace)

    assert payload["entry_count"] == 3
    assert [entry["id"] for entry in payload["entries"]] == [
        "idiom-00001",
        "idiom-00003",
        "idiom-00005",
    ]
    assert payload["manifest"]["manual_review"]["legacy_schema_files"] == 1


def test_validate_curated_pack_reports_matching_keep_catalog_and_level_usage(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"
    _write_review_file(
        workspace,
        "batch-0001",
        [
            {"global_seq": 1, "idiom_id": "idiom-00001", "text": "一心一意", "decision": "keep", "note": ""},
            {"global_seq": 2, "idiom_id": "idiom-00002", "text": "一团和气", "review": "keep", "note": ""},
            {"global_seq": 3, "idiom_id": "idiom-00003", "text": "一马当先", "decision": "keep", "note": ""},
            {"global_seq": 4, "idiom_id": "idiom-00004", "text": "一举两得", "review": "keep", "note": ""},
            {"global_seq": 5, "idiom_id": "idiom-00005", "text": "一知半解", "decision": "filter", "note": "偏弱"},
        ],
    )

    curated_payload = build_curated_catalog_payload(catalog_path=catalog_path, workspace=workspace)
    curated_catalog_path = tmp_path / "final_common_idioms.json"
    curated_catalog_path.write_text(json.dumps(curated_payload, ensure_ascii=False, indent=2), encoding="utf-8")

    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    level_rows = [
        {
            "level_id": "level-0001",
            "chapter_id": "chapter-001",
            "idiom_ids": ["idiom-00001", "idiom-00002"],
            "board_width": 6,
            "board_height": 6,
            "candidate_chars": [],
            "placements": [],
        },
        {
            "level_id": "level-0002",
            "chapter_id": "chapter-001",
            "idiom_ids": ["idiom-00003", "idiom-00004"],
            "board_width": 6,
            "board_height": 6,
            "candidate_chars": [],
            "placements": [],
        },
    ]
    for row in level_rows:
        (levels_dir / f"{row['level_id']}.json").write_text(
            json.dumps(row, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    chapter_index = {
        "chapters": [
            {
                "chapter_id": "chapter-001",
                "title": "第一章",
                "level_ids": ["level-0001", "level-0002"],
                "level_count": 2,
                "first_level_id": "level-0001",
            }
        ],
        "total_levels": 2,
    }
    chapter_index_path = tmp_path / "chapters.json"
    chapter_index_path.write_text(json.dumps(chapter_index, ensure_ascii=False, indent=2), encoding="utf-8")

    errors, report = validate_curated_pack(
        workspace=workspace,
        catalog_path=curated_catalog_path,
        chapter_index_path=chapter_index_path,
        levels_dir=levels_dir,
        expected_levels=2,
    )

    assert errors == []
    assert report["keep_count"] == 4
    assert report["catalog_entry_count"] == 4
    assert report["used_unique_idiom_count"] == 4


def test_validate_curated_pack_rejects_unique_idiom_mismatch(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"
    _write_review_file(
        workspace,
        "batch-0001",
        [
            {"global_seq": 1, "idiom_id": "idiom-00001", "text": "一心一意", "decision": "keep", "note": ""},
            {"global_seq": 2, "idiom_id": "idiom-00002", "text": "一团和气", "decision": "keep", "note": ""},
            {"global_seq": 3, "idiom_id": "idiom-00003", "text": "一马当先", "decision": "keep", "note": ""},
            {"global_seq": 4, "idiom_id": "idiom-00004", "text": "一举两得", "decision": "filter", "note": "不保留"},
            {"global_seq": 5, "idiom_id": "idiom-00005", "text": "一知半解", "decision": "filter", "note": "不保留"},
        ],
    )

    curated_payload = build_curated_catalog_payload(catalog_path=catalog_path, workspace=workspace)
    curated_catalog_path = tmp_path / "final_common_idioms.json"
    curated_catalog_path.write_text(json.dumps(curated_payload, ensure_ascii=False, indent=2), encoding="utf-8")

    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    (levels_dir / "level-0001.json").write_text(
        json.dumps(
            {
                "level_id": "level-0001",
                "chapter_id": "chapter-001",
                "idiom_ids": ["idiom-00001", "idiom-00002"],
                "board_width": 6,
                "board_height": 6,
                "candidate_chars": [],
                "placements": [],
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    chapter_index_path = tmp_path / "chapters.json"
    chapter_index_path.write_text(
        json.dumps(
            {
                "chapters": [
                    {
                        "chapter_id": "chapter-001",
                        "title": "第一章",
                        "level_ids": ["level-0001"],
                        "level_count": 1,
                        "first_level_id": "level-0001",
                    }
                ],
                "total_levels": 1,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    errors, _ = validate_curated_pack(
        workspace=workspace,
        catalog_path=curated_catalog_path,
        chapter_index_path=chapter_index_path,
        levels_dir=levels_dir,
        expected_levels=1,
    )

    assert any("唯一成语数量" in error for error in errors)
