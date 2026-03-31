import json

from word_dragon_content.content_validator import validate_content_bundle


def test_validate_content_bundle_accepts_valid_pack(tmp_path):
    catalog = [
        {"id": "i001", "text": "高山流水", "enabled": True},
        {"id": "i002", "text": "水到渠成", "enabled": True},
        {"id": "i003", "text": "成竹在胸", "enabled": True},
        {"id": "i004", "text": "胸有成竹", "enabled": True},
    ]
    level = {
        "level_id": "level-0001",
        "chapter_id": "chapter-001",
        "idiom_ids": ["i001", "i002", "i003", "i004"],
        "board_width": 7,
        "board_height": 8,
        "candidate_chars": ["高", "山", "流", "水", "到", "渠", "成", "成", "竹", "竹", "在", "胸", "有"],
        "placements": [
            {"idiom_id": "i001", "orientation": "across", "row": 1, "col": 0},
            {"idiom_id": "i002", "orientation": "down", "row": 1, "col": 3},
            {"idiom_id": "i003", "orientation": "across", "row": 4, "col": 3},
            {"idiom_id": "i004", "orientation": "down", "row": 4, "col": 6},
        ],
    }
    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    (levels_dir / "level-0001.json").write_text(
        json.dumps(level, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    chapters = {
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
    }

    errors = validate_content_bundle(
        catalog=catalog,
        chapter_index=chapters,
        levels_dir=levels_dir,
        min_levels=1,
        max_idioms_per_level=8,
    )

    assert errors == []


def test_validate_content_bundle_rejects_duplicate_and_oversized_levels(tmp_path):
    catalog = [{"id": f"i{index:03d}", "text": "高山流水", "enabled": True} for index in range(1, 10)]
    level = {
        "level_id": "level-0001",
        "chapter_id": "chapter-001",
        "idiom_ids": [entry["id"] for entry in catalog],
        "board_width": 4,
        "board_height": 4,
        "candidate_chars": ["高", "山", "流", "水"],
        "placements": [
            {"idiom_id": entry["id"], "orientation": "across", "row": 0, "col": 0}
            for entry in catalog
        ],
    }
    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    payload = json.dumps(level, ensure_ascii=False, indent=2)
    (levels_dir / "level-0001.json").write_text(payload, encoding="utf-8")
    (levels_dir / "level-0002.json").write_text(payload.replace("level-0001", "level-0002"), encoding="utf-8")
    chapters = {
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

    errors = validate_content_bundle(
        catalog=catalog,
        chapter_index=chapters,
        levels_dir=levels_dir,
        min_levels=2,
        max_idioms_per_level=8,
    )

    assert any("超过 8 个成语" in error for error in errors)
    assert any("重复关卡" in error for error in errors)


def test_validate_content_bundle_rejects_under_provisioned_candidate_frequency(tmp_path):
    catalog = [
        {"id": "i001", "text": "一世两清", "enabled": True},
        {"id": "i002", "text": "一国两制", "enabled": True},
        {"id": "i003", "text": "一物一制", "enabled": True},
        {"id": "i004", "text": "一夕五制", "enabled": True},
    ]
    level = {
        "level_id": "level-0001",
        "chapter_id": "chapter-001",
        "idiom_ids": ["i001", "i002", "i003", "i004"],
        "board_width": 7,
        "board_height": 7,
        "candidate_chars": ["一", "世", "两", "清", "国", "制", "物", "夕", "五"],
        "placements": [
            {"idiom_id": "i001", "orientation": "across", "row": 0, "col": 3},
            {"idiom_id": "i002", "orientation": "down", "row": 0, "col": 3},
            {"idiom_id": "i003", "orientation": "across", "row": 3, "col": 0},
            {"idiom_id": "i004", "orientation": "down", "row": 3, "col": 0},
        ],
    }
    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    (levels_dir / "level-0001.json").write_text(
        json.dumps(level, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    chapters = {
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
    }

    errors = validate_content_bundle(
        catalog=catalog,
        chapter_index=chapters,
        levels_dir=levels_dir,
        min_levels=1,
        max_idioms_per_level=8,
    )

    assert any("候选字盘缺少足够的" in error for error in errors)
