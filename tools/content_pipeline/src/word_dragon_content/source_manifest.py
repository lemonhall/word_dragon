from __future__ import annotations

from pathlib import Path


PACKAGE_ROOT = Path(__file__).resolve().parents[2]
RAW_ROOT = PACKAGE_ROOT / "data" / "raw" / "mapull"


def build_source_manifest() -> dict:
    return {
        "dictionary": {
            "id": "mapull-chinese-dictionary",
            "repo": "https://github.com/mapull/chinese-dictionary",
            "license": "MIT",
            "path": str(RAW_ROOT / "idiom.json"),
            "notes": "Upstream README warns that some collected data may have uncertain earliest origins; Word Dragon applies local filtering and short explanation rewriting before shipping.",
        },
        "character_frequency": {
            "id": "mapull-char-common-base",
            "repo": "https://github.com/mapull/chinese-dictionary",
            "path": str(RAW_ROOT / "char_common_base.json"),
            "scale": "0=most common, 1=common, 2=secondary common, 3+=less suitable for senior-first packs",
        },
        "curation_policy": {
            "four_character_only": True,
            "strict_max_character_frequency": 2,
            "strict_max_frequency_rank": 5,
            "short_explanation_style": "Use the first concise sentence and keep it suitable for large-font UI.",
        },
    }


def raw_dictionary_path() -> Path:
    return RAW_ROOT / "idiom.json"


def raw_character_frequency_path() -> Path:
    return RAW_ROOT / "char_common_base.json"
