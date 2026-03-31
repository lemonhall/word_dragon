from __future__ import annotations

import json
import re
from pathlib import Path


_CJK_RE = re.compile(r"^[\u4e00-\u9fff]{4}$")


def load_raw_idioms(path: Path) -> list[dict]:
    return json.loads(path.read_text(encoding="utf-8"))


def load_character_frequency(path: Path) -> dict[str, int]:
    text = path.read_text(encoding="utf-8")
    entries: dict[str, int] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip().rstrip(",")
        if not line:
            continue
        payload = json.loads(line)
        entries[payload["char"]] = int(payload["frequency"])
    return entries


def build_catalog(
    raw_rows: list[dict],
    char_frequency: dict[str, int],
    strict: bool = True,
) -> list[dict]:
    threshold = 2 if strict else 3
    rank_limit = 5 if strict else 8
    deduped: dict[str, dict] = {}

    for row in raw_rows:
        word = str(row.get("word", "")).strip()
        if not _CJK_RE.fullmatch(word):
            continue

        frequencies = [char_frequency.get(char, 5) for char in word]
        if max(frequencies) > threshold:
            continue

        frequency_rank = sum(frequencies)
        if strict and frequency_rank > rank_limit:
            continue

        explanation = str(row.get("explanation", "")).strip()
        if not explanation:
            continue

        short_explanation = shorten_explanation(explanation)
        candidate = {
            "id": "",
            "text": word,
            "pinyin": str(row.get("pinyin", "")).strip(),
            "short_explanation": short_explanation,
            "tts_text": f"{word}。{short_explanation}",
            "frequency_rank": frequency_rank,
            "difficulty_tier": classify_difficulty(frequency_rank),
            "enabled": True,
        }

        previous = deduped.get(word)
        if previous is None or previous["frequency_rank"] > candidate["frequency_rank"]:
            deduped[word] = candidate

    ordered = sorted(deduped.values(), key=lambda entry: (entry["frequency_rank"], entry["text"]))
    for index, entry in enumerate(ordered, start=1):
        entry["id"] = f"idiom-{index:05d}"
    return ordered


def shorten_explanation(text: str, max_chars: int = 22) -> str:
    cleaned = re.sub(r"\s+", "", text)
    parts = [part.strip() for part in re.split(r"[。！？!?；;]", cleaned) if part.strip()]
    body = parts[0] if parts else cleaned
    if len(body) > max_chars - 1:
        body = body[: max_chars - 1].rstrip("，,、；;：:")
    return f"{body}。"


def classify_difficulty(frequency_rank: int) -> str:
    if frequency_rank <= 3:
        return "starter"
    if frequency_rank <= 5:
        return "standard"
    return "advanced"
