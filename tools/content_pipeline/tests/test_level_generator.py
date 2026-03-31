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
