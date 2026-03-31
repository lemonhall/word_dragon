from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

from word_dragon_content.level_generator import calculate_required_candidate_counts, is_level_connected


def validate_content_bundle(
    catalog: list[dict],
    chapter_index: dict,
    levels_dir: Path,
    min_levels: int,
    max_idioms_per_level: int,
) -> list[str]:
    errors: list[str] = []
    catalog_map = {entry["id"]: entry for entry in catalog}

    for entry in catalog:
        if entry.get("enabled") and len(entry["text"]) != 4:
            errors.append(f"词条 {entry['id']} 不是四字成语。")

    levels = load_levels(levels_dir)
    levels_by_id = {level["level_id"]: level for level in levels}

    if chapter_index.get("total_levels", 0) < min_levels:
        errors.append(f"章节索引统计关卡数低于 {min_levels}。")
    if len(levels) < min_levels:
        errors.append(f"关卡文件数低于 {min_levels}。")

    seen_signatures: set[tuple[str, ...]] = set()
    for level in levels:
        idiom_ids = level.get("idiom_ids", [])
        if len(idiom_ids) > max_idioms_per_level:
            errors.append(f"{level['level_id']} 超过 {max_idioms_per_level} 个成语。")
        if len(idiom_ids) < 4:
            errors.append(f"{level['level_id']} 少于 4 个成语。")
        if len(set(idiom_ids)) != len(idiom_ids):
            errors.append(f"{level['level_id']} 包含重复成语。")
        if len(level.get("placements", [])) != len(idiom_ids):
            errors.append(f"{level['level_id']} 的 placements 数量与 idiom_ids 不一致。")

        signature = tuple(sorted(idiom_ids))
        if signature in seen_signatures:
            errors.append(f"{level['level_id']} 是重复关卡。")
        seen_signatures.add(signature)

        for idiom_id in idiom_ids:
            entry = catalog_map.get(idiom_id)
            if entry is None:
                errors.append(f"{level['level_id']} 引用了不存在的词条 {idiom_id}。")
        texts_by_id = {
            idiom_id: catalog_map[idiom_id]["text"]
            for idiom_id in idiom_ids
            if idiom_id in catalog_map
        }
        required_counts = calculate_required_candidate_counts(level.get("placements", []), texts_by_id)
        candidate_counts = Counter(level.get("candidate_chars", []))
        for char, required_count in required_counts.items():
            actual_count = candidate_counts[char]
            if actual_count < required_count:
                errors.append(
                    f"{level['level_id']} 的候选字盘缺少足够的 {char}，至少需要 {required_count} 个，实际只有 {actual_count} 个。"
                )

        if not is_level_connected(level, catalog):
            errors.append(f"{level['level_id']} 不是连通字盘。")

    for chapter in chapter_index.get("chapters", []):
        level_ids = chapter.get("level_ids", [])
        if chapter.get("level_count") != len(level_ids):
            errors.append(f"{chapter['chapter_id']} 的 level_count 与 level_ids 数量不一致。")
        if level_ids and chapter.get("first_level_id") != level_ids[0]:
            errors.append(f"{chapter['chapter_id']} 的 first_level_id 不匹配。")
        for level_id in level_ids:
            if level_id not in levels_by_id:
                errors.append(f"{chapter['chapter_id']} 引用了缺失关卡 {level_id}。")

    return errors


def load_levels(levels_dir: Path) -> list[dict]:
    levels: list[dict] = []
    for path in sorted(levels_dir.glob("*.json")):
        levels.append(json.loads(path.read_text(encoding="utf-8")))
    return levels
