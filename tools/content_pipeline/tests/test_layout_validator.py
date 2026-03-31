import json

from word_dragon_content.layout_validator import UiLayoutProfile, validate_layout_bundle


def test_validate_layout_bundle_accepts_reference_pack(tmp_path):
    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
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
    (levels_dir / "level-0001.json").write_text(
        json.dumps(level, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    chapter_index = {
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
    profile = UiLayoutProfile(
        min_board_cell_sp=28,
        min_touch_target_dp=56,
        primary_button_height_dp=72,
        compact_button_height_dp=60,
        comfortable_screen_padding_dp=24,
        compact_screen_padding_dp=16,
        comfortable_section_spacing_dp=18,
        compact_section_spacing_dp=12,
        board_weight=1.1,
        controls_weight=0.9,
        compact_padding_dp=12,
        comfortable_padding_dp=20,
        compact_spacing_dp=4,
        comfortable_spacing_dp=8,
        preferred_cell_size_dp=56,
        minimum_readable_cell_size_dp=34,
    )

    errors, report = validate_layout_bundle(
        chapter_index=chapter_index,
        levels_dir=levels_dir,
        profile=profile,
        min_cell_sp=28,
        min_touch_dp=56,
    )

    assert errors == []
    assert report["total_levels"] == 1
    assert report["max_board_width"] == 7
    assert report["portrait_scroll_levels"] == 0


def test_validate_layout_bundle_rejects_under_sized_tokens(tmp_path):
    levels_dir = tmp_path / "levels"
    levels_dir.mkdir()
    (levels_dir / "level-0001.json").write_text(
        json.dumps(
            {
                "level_id": "level-0001",
                "chapter_id": "chapter-001",
                "idiom_ids": ["i001", "i002", "i003", "i004"],
                "board_width": 6,
                "board_height": 6,
                "candidate_chars": ["高", "山", "流", "水"],
                "placements": [],
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    chapter_index = {
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
    profile = UiLayoutProfile(
        min_board_cell_sp=26,
        min_touch_target_dp=52,
        primary_button_height_dp=54,
        compact_button_height_dp=52,
        comfortable_screen_padding_dp=24,
        compact_screen_padding_dp=16,
        comfortable_section_spacing_dp=18,
        compact_section_spacing_dp=12,
        board_weight=1.1,
        controls_weight=0.9,
        compact_padding_dp=12,
        comfortable_padding_dp=20,
        compact_spacing_dp=4,
        comfortable_spacing_dp=8,
        preferred_cell_size_dp=56,
        minimum_readable_cell_size_dp=34,
    )

    errors, _ = validate_layout_bundle(
        chapter_index=chapter_index,
        levels_dir=levels_dir,
        profile=profile,
        min_cell_sp=28,
        min_touch_dp=56,
    )

    assert any("棋盘字号" in error for error in errors)
    assert any("最小触控尺寸" in error for error in errors)
    assert any("主要操作按钮高度" in error for error in errors)
    assert any("紧凑模式按钮高度" in error for error in errors)
