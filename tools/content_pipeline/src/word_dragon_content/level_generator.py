from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path


def generate_level_pack(
    catalog: list[dict],
    min_levels: int,
    max_idioms_per_level: int,
    chapter_size: int,
) -> tuple[list[dict], list[dict]]:
    if max_idioms_per_level < 4:
        raise ValueError("max_idioms_per_level must be at least 4")

    enabled_entries = sorted(
        (entry for entry in catalog if entry.get("enabled")),
        key=lambda entry: (entry["frequency_rank"], entry["text"]),
    )
    char_index = build_character_index(enabled_entries)

    levels: list[dict] = []
    seen_signatures: set[tuple[str, ...]] = set()
    idioms_per_level = min(4, max_idioms_per_level)

    for seed in enabled_entries:
        if len(levels) >= min_levels:
            break
        chain = find_chain(seed, char_index, idioms_per_level)
        if chain is None:
            continue
        signature = tuple(sorted(entry["id"] for entry in chain))
        if signature in seen_signatures:
            continue
        level = build_level(chain, len(levels) + 1)
        if level is None:
            continue
        seen_signatures.add(signature)
        levels.append(level)

    if len(levels) < min_levels:
        raise RuntimeError(f"Unable to generate the requested {min_levels} levels from the current catalog.")

    chapters = build_chapters(levels, chapter_size)
    return levels, chapters


def build_character_index(catalog: list[dict]) -> dict[str, list[dict]]:
    index: dict[str, list[dict]] = defaultdict(list)
    for entry in catalog:
        for char in set(entry["text"]):
            index[char].append(entry)
    for char_entries in index.values():
        char_entries.sort(key=lambda entry: (entry["frequency_rank"], entry["text"]))
    return index


def find_chain(seed: dict, char_index: dict[str, list[dict]], target_length: int) -> list[dict] | None:
    chain = [seed]
    used_ids = {seed["id"]}

    def search() -> list[dict] | None:
        if len(chain) == target_length:
            return list(chain)

        for candidate in iter_neighbors(chain[-1], char_index):
            if candidate["id"] in used_ids:
                continue
            chain.append(candidate)
            if try_layout(chain) is not None:
                used_ids.add(candidate["id"])
                result = search()
                if result is not None:
                    return result
                used_ids.remove(candidate["id"])
            chain.pop()
        return None

    return search()


def iter_neighbors(entry: dict, char_index: dict[str, list[dict]], limit: int = 48):
    seen_ids: set[str] = set()
    emitted = 0
    for char in sorted(set(entry["text"]), key=lambda value: (len(char_index[value]), value)):
        for candidate in char_index[char]:
            if candidate["id"] == entry["id"] or candidate["id"] in seen_ids:
                continue
            seen_ids.add(candidate["id"])
            yield candidate
            emitted += 1
            if emitted >= limit:
                return


def build_level(chain: list[dict], sequence: int) -> dict | None:
    placements = try_layout(chain)
    if placements is None:
        return None

    row_values = [placement["row"] for placement in placements]
    col_values = [placement["col"] for placement in placements]
    width = 0
    height = 0
    normalized_placements = []
    min_row = min(row_values)
    min_col = min(col_values)

    for placement in placements:
        row = placement["row"] - min_row
        col = placement["col"] - min_col
        normalized = {
            "idiom_id": placement["idiom_id"],
            "orientation": placement["orientation"],
            "row": row,
            "col": col,
        }
        normalized_placements.append(normalized)
        text = placement["text"]
        if placement["orientation"] == "across":
            width = max(width, col + len(text))
            height = max(height, row + 1)
        else:
            width = max(width, col + 1)
            height = max(height, row + len(text))

    ordered_chars: list[str] = []
    for entry in chain:
        for char in entry["text"]:
            if char not in ordered_chars:
                ordered_chars.append(char)
    required_counts = calculate_required_candidate_counts(
        placements=normalized_placements,
        texts_by_id={entry["id"]: entry["text"] for entry in chain},
    )
    candidate_chars: list[str] = []
    for char in ordered_chars:
        candidate_chars.extend([char] * required_counts[char])

    chapter_number = ((sequence - 1) // 50) + 1
    return {
        "level_id": f"level-{sequence:04d}",
        "chapter_id": f"chapter-{chapter_number:03d}",
        "idiom_ids": [entry["id"] for entry in chain],
        "board_width": width,
        "board_height": height,
        "candidate_chars": candidate_chars,
        "layout_profile": "chain-4",
        "placements": normalized_placements,
    }


def calculate_required_candidate_counts(
    placements: list[dict],
    texts_by_id: dict[str, str],
) -> Counter[str]:
    counts: Counter[str] = Counter()
    seen_coordinates: set[tuple[int, int]] = set()
    for placement in placements:
        text = texts_by_id[placement["idiom_id"]]
        for index, char in enumerate(text):
            row = placement["row"] + (index if placement["orientation"] == "down" else 0)
            col = placement["col"] + (index if placement["orientation"] == "across" else 0)
            coordinate = (row, col)
            if coordinate in seen_coordinates:
                continue
            seen_coordinates.add(coordinate)
            counts[char] += 1
    return counts


def try_layout(chain: list[dict]) -> list[dict] | None:
    placements: list[dict] = []
    occupied: dict[tuple[int, int], str] = {}
    used_indexes_by_id: dict[str, set[int]] = {}

    first = chain[0]
    first_placement = {
        "idiom_id": first["id"],
        "text": first["text"],
        "orientation": "across",
        "row": 3,
        "col": 0,
    }
    add_word(first_placement, occupied)
    placements.append(first_placement)
    used_indexes_by_id[first["id"]] = set()

    for entry in chain[1:]:
        previous = placements[-1]
        orientation = "down" if previous["orientation"] == "across" else "across"
        option = find_placement_option(
            previous=previous,
            current=entry,
            orientation=orientation,
            occupied=occupied,
            used_indexes=used_indexes_by_id,
        )
        if option is None:
            return None

        previous["used_index"] = option["previous_index"]
        used_indexes_by_id.setdefault(previous["idiom_id"], set()).add(option["previous_index"])
        placement = {
            "idiom_id": entry["id"],
            "text": entry["text"],
            "orientation": orientation,
            "row": option["row"],
            "col": option["col"],
        }
        add_word(placement, occupied)
        placements.append(placement)
        used_indexes_by_id[entry["id"]] = {option["current_index"]}

    return placements


def find_placement_option(
    previous: dict,
    current: dict,
    orientation: str,
    occupied: dict[tuple[int, int], str],
    used_indexes: dict[str, set[int]],
) -> dict | None:
    previous_text = previous["text"]
    current_text = current["text"]

    for previous_index, current_index in shared_char_pairs(previous_text, current_text):
        if previous_index in used_indexes.get(previous["idiom_id"], set()):
            continue

        intersection_row, intersection_col = cell_at(previous, previous_index)
        row = intersection_row - (current_index if orientation == "down" else 0)
        col = intersection_col - (current_index if orientation == "across" else 0)

        if not placement_conflicts(current_text, row, col, orientation, occupied, (intersection_row, intersection_col)):
            return {
                "row": row,
                "col": col,
                "previous_index": previous_index,
                "current_index": current_index,
            }
    return None


def shared_char_pairs(previous_text: str, current_text: str) -> list[tuple[int, int]]:
    pairs: list[tuple[int, int]] = []
    for previous_index, previous_char in enumerate(previous_text):
        for current_index, current_char in enumerate(current_text):
            if previous_char == current_char:
                pairs.append((previous_index, current_index))
    return pairs


def placement_conflicts(
    text: str,
    row: int,
    col: int,
    orientation: str,
    occupied: dict[tuple[int, int], str],
    allowed_overlap: tuple[int, int],
) -> bool:
    for index, char in enumerate(text):
        cell = (row, col + index) if orientation == "across" else (row + index, col)
        existing = occupied.get(cell)
        if existing is None:
            continue
        if cell != allowed_overlap or existing != char:
            return True
    return False


def add_word(placement: dict, occupied: dict[tuple[int, int], str]) -> None:
    for index, char in enumerate(placement["text"]):
        cell = cell_at(placement, index)
        occupied[cell] = char


def cell_at(placement: dict, index: int) -> tuple[int, int]:
    if placement["orientation"] == "across":
        return placement["row"], placement["col"] + index
    return placement["row"] + index, placement["col"]


def build_chapters(levels: list[dict], chapter_size: int) -> list[dict]:
    chapters: list[dict] = []
    for offset in range(0, len(levels), chapter_size):
        group = levels[offset : offset + chapter_size]
        chapter_number = (offset // chapter_size) + 1
        chapters.append(
            {
                "chapter_id": f"chapter-{chapter_number:03d}",
                "title": f"第{chapter_number}章 常用成语练习",
                "level_ids": [level["level_id"] for level in group],
                "level_count": len(group),
                "first_level_id": group[0]["level_id"],
            }
        )
    return chapters


def is_level_connected(level: dict, catalog: list[dict]) -> bool:
    catalog_map = {entry["id"]: entry["text"] for entry in catalog}
    placements = level["placements"]
    if not placements:
        return False

    cells_by_id: dict[str, set[tuple[int, int]]] = {}
    for placement in placements:
        text = catalog_map[placement["idiom_id"]]
        cells = {
            (placement["row"], placement["col"] + index)
            if placement["orientation"] == "across"
            else (placement["row"] + index, placement["col"])
            for index, _ in enumerate(text)
        }
        cells_by_id[placement["idiom_id"]] = cells

    graph: dict[str, set[str]] = {placement["idiom_id"]: set() for placement in placements}
    placement_ids = list(cells_by_id)
    for index, left_id in enumerate(placement_ids):
        for right_id in placement_ids[index + 1 :]:
            if cells_by_id[left_id] & cells_by_id[right_id]:
                graph[left_id].add(right_id)
                graph[right_id].add(left_id)

    visited = set()
    stack = [placement_ids[0]]
    while stack:
        current = stack.pop()
        if current in visited:
            continue
        visited.add(current)
        stack.extend(graph[current] - visited)
    return len(visited) == len(placements)


def write_levels(output_dir: Path, levels: list[dict]) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    for path in output_dir.glob("*.json"):
        path.unlink()
    for level in levels:
        (output_dir / f"{level['level_id']}.json").write_text(
            json.dumps(level, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
