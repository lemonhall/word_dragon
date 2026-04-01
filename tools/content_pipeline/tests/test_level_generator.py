from collections import Counter

from word_dragon_content.level_generator import generate_level_pack, is_level_connected


def test_generate_level_pack_builds_connected_chapters():
    catalog = [
        {
            "id": "i001",
            "text": "高山流水",
            "pinyin": "gao shan liu shui",
            "short_explanation": "比喻知音难遇。",
            "tts_text": "高山流水。比喻知音难遇。",
            "frequency_rank": 1,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i002",
            "text": "水到渠成",
            "pinyin": "shui dao qu cheng",
            "short_explanation": "条件成熟后自然成功。",
            "tts_text": "水到渠成。条件成熟后自然成功。",
            "frequency_rank": 2,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i003",
            "text": "成竹在胸",
            "pinyin": "cheng zhu zai xiong",
            "short_explanation": "心里早有完整打算。",
            "tts_text": "成竹在胸。心里早有完整打算。",
            "frequency_rank": 2,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i004",
            "text": "胸有成竹",
            "pinyin": "xiong you cheng zhu",
            "short_explanation": "做事前已有把握。",
            "tts_text": "胸有成竹。做事前已有把握。",
            "frequency_rank": 2,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i005",
            "text": "竹报平安",
            "pinyin": "zhu bao ping an",
            "short_explanation": "借平安消息报喜。",
            "tts_text": "竹报平安。借平安消息报喜。",
            "frequency_rank": 2,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i006",
            "text": "安居乐业",
            "pinyin": "an ju le ye",
            "short_explanation": "生活安定，工作愉快。",
            "tts_text": "安居乐业。生活安定，工作愉快。",
            "frequency_rank": 1,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i007",
            "text": "业精于勤",
            "pinyin": "ye jing yu qin",
            "short_explanation": "学业因勤奋而精进。",
            "tts_text": "业精于勤。学业因勤奋而精进。",
            "frequency_rank": 1,
            "difficulty_tier": "starter",
            "enabled": True,
        },
        {
            "id": "i008",
            "text": "勤能补拙",
            "pinyin": "qin neng bu zhuo",
            "short_explanation": "勤奋可以弥补不足。",
            "tts_text": "勤能补拙。勤奋可以弥补不足。",
            "frequency_rank": 1,
            "difficulty_tier": "starter",
            "enabled": True,
        },
    ]

    levels, chapters = generate_level_pack(
        catalog,
        min_levels=2,
        max_idioms_per_level=4,
        chapter_size=1,
    )

    assert len(levels) == 2
    assert len(chapters) == 2
    assert chapters[0]["level_count"] == 1
    assert chapters[0]["first_level_id"] == levels[0]["level_id"]
    assert all(len(level["idiom_ids"]) == 4 for level in levels)
    assert all(is_level_connected(level, catalog) for level in levels)
    assert all(level["board_width"] >= 4 for level in levels)
    assert all(level["board_height"] >= 4 for level in levels)


def test_generate_level_pack_dedupes_same_idiom_set_in_different_order():
    catalog = [
        {"id": "i001", "text": "一个半个", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i002", "text": "一个心眼", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i003", "text": "个中之人", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i004", "text": "中天之世", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i005", "text": "世外桃源", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i006", "text": "源头活水", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
    ]

    levels, _ = generate_level_pack(
        catalog,
        min_levels=2,
        max_idioms_per_level=4,
        chapter_size=1,
    )

    signatures = {tuple(sorted(level["idiom_ids"])) for level in levels}
    assert len(signatures) == len(levels)


def test_generate_level_pack_preserves_candidate_frequency_needed_by_board():
    catalog = [
        {"id": "i001", "text": "一世两清", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i002", "text": "一国两制", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i003", "text": "一物一制", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i004", "text": "一夕五制", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
    ]

    levels, _ = generate_level_pack(
        catalog,
        min_levels=1,
        max_idioms_per_level=4,
        chapter_size=1,
    )

    level = levels[0]
    texts_by_id = {entry["id"]: entry["text"] for entry in catalog}
    required_counts: Counter[str] = Counter()
    seen_coordinates: dict[tuple[int, int], str] = {}
    for placement in level["placements"]:
        text = texts_by_id[placement["idiom_id"]]
        for index, char in enumerate(text):
            row = placement["row"] + (index if placement["orientation"] == "down" else 0)
            col = placement["col"] + (index if placement["orientation"] == "across" else 0)
            coordinate = (row, col)
            if coordinate not in seen_coordinates:
                seen_coordinates[coordinate] = char
                required_counts[char] += 1

    candidate_counts = Counter(level["candidate_chars"])
    for char, count in required_counts.items():
        assert candidate_counts[char] >= count


def test_generate_level_pack_covers_full_catalog_before_reuse():
    catalog = [
        {"id": "i001", "text": "高山流水", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i002", "text": "水到渠成", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i003", "text": "成竹在胸", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i004", "text": "胸有成竹", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i005", "text": "竹报平安", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i006", "text": "安居乐业", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i007", "text": "业精于勤", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i008", "text": "勤能补拙", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
    ]

    levels, _ = generate_level_pack(
        catalog,
        min_levels=4,
        max_idioms_per_level=4,
        chapter_size=2,
        preferred_idioms_per_level=4,
        require_full_catalog_coverage=True,
        max_board_width=7,
    )

    used_unique_ids = {idiom_id for level in levels for idiom_id in level["idiom_ids"]}
    assert len(levels) == 4
    assert used_unique_ids == {entry["id"] for entry in catalog}


def test_generate_level_pack_respects_max_board_width():
    catalog = [
        {"id": "i001", "text": "高山流水", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i002", "text": "水到渠成", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i003", "text": "成竹在胸", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i004", "text": "胸有成竹", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i005", "text": "竹报平安", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i006", "text": "安居乐业", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i007", "text": "业精于勤", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
        {"id": "i008", "text": "勤能补拙", "pinyin": "", "short_explanation": "", "tts_text": "", "frequency_rank": 0, "difficulty_tier": "starter", "enabled": True},
    ]

    levels, _ = generate_level_pack(
        catalog,
        min_levels=1,
        max_idioms_per_level=8,
        chapter_size=1,
        preferred_idioms_per_level=6,
        max_board_width=9,
    )

    assert levels[0]["board_width"] <= 9
