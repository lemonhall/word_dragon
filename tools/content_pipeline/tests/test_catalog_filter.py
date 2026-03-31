from word_dragon_content.catalog_filter import build_catalog
from word_dragon_content.source_manifest import build_source_manifest


def test_build_catalog_keeps_only_four_character_common_idioms():
    raw_rows = [
        {
            "word": "高山流水",
            "pinyin": "gao shan liu shui",
            "explanation": "比喻知音难遇。也比喻乐曲高妙。",
        },
        {
            "word": "魑魅魍魉",
            "pinyin": "chi mei wang liang",
            "explanation": "原为传说中的鬼怪。后比喻各种坏人。",
        },
        {
            "word": "一不做，二不休",
            "pinyin": "yi bu zuo er bu xiu",
            "explanation": "事情做了开头就索性做到底。",
        },
    ]
    char_frequency = {
        "高": 1,
        "山": 0,
        "流": 0,
        "水": 0,
        "魑": 5,
        "魅": 5,
        "魍": 5,
        "魉": 5,
        "一": 0,
        "不": 0,
        "做": 1,
        "二": 0,
        "休": 1,
    }

    entries = build_catalog(raw_rows, char_frequency, strict=True)

    assert [entry["text"] for entry in entries] == ["高山流水"]
    assert entries[0]["frequency_rank"] == 1
    assert entries[0]["difficulty_tier"] == "starter"
    assert entries[0]["short_explanation"] == "比喻知音难遇。"
    assert entries[0]["tts_text"] == "高山流水。比喻知音难遇。"
    assert entries[0]["enabled"] is True


def test_source_manifest_records_local_upstream_choices():
    manifest = build_source_manifest()

    assert manifest["dictionary"]["id"] == "mapull-chinese-dictionary"
    assert manifest["dictionary"]["license"] == "MIT"
    assert manifest["character_frequency"]["path"].endswith("char_common_base.json")
