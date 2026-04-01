from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path


def generate_level_pack(
    catalog: list[dict],
    min_levels: int,
    max_idioms_per_level: int,
    chapter_size: int,
    *,
    preferred_idioms_per_level: int | None = None,
    require_full_catalog_coverage: bool = False,
    max_board_width: int | None = None,
) -> tuple[list[dict], list[dict]]:
    if max_idioms_per_level < 4:
        raise ValueError("max_idioms_per_level must be at least 4")

    enabled_entries = sorted(
        (entry for entry in catalog if entry.get("enabled")),
        key=lambda entry: (entry["frequency_rank"], entry["text"]),
    )
    if not enabled_entries:
        raise ValueError("catalog does not contain enabled idioms")

    idioms_per_level = preferred_idioms_per_level or min(4, max_idioms_per_level)
    if idioms_per_level < 4 or idioms_per_level > max_idioms_per_level:
        raise ValueError("preferred_idioms_per_level must stay within [4, max_idioms_per_level]")

    char_index = build_character_index(enabled_entries)
    levels: list[dict] = []
    seen_signatures: set[tuple[str, ...]] = set()

    if require_full_catalog_coverage:
        coverage_chains = build_coverage_chains(
            enabled_entries=enabled_entries,
            char_index=char_index,
            target_length=idioms_per_level,
            max_board_width=max_board_width,
        )
        if len(coverage_chains) > min_levels:
            raise RuntimeError(
                f"Need {len(coverage_chains)} levels to cover the catalog, which exceeds requested {min_levels}."
            )
        for chain in coverage_chains:
            signature = tuple(sorted(entry["id"] for entry in chain))
            level = build_level(chain, len(levels) + 1, max_board_width=max_board_width)
            if level is None:
                raise RuntimeError("Coverage chain could not be converted into a valid level.")
            seen_signatures.add(signature)
            levels.append(level)

    while len(levels) < min_levels:
        generated_in_round = False
        for seed in enabled_entries:
            chain = find_chain(
                seed=seed,
                char_index=char_index,
                target_length=idioms_per_level,
                max_board_width=max_board_width,
            )
            if chain is None:
                continue
            signature = tuple(sorted(entry["id"] for entry in chain))
            if signature in seen_signatures:
                continue
            level = build_level(chain, len(levels) + 1, max_board_width=max_board_width)
            if level is None:
                continue
            seen_signatures.add(signature)
            levels.append(level)
            generated_in_round = True
            if len(levels) >= min_levels:
                break
        if not generated_in_round:
            raise RuntimeError(f"Unable to generate the requested {min_levels} levels from the current catalog.")

    chapters = build_chapters(levels, chapter_size)
    return levels, chapters


def build_coverage_chains(
    *,
    enabled_entries: list[dict],
    char_index: dict[str, list[dict]],
    target_length: int,
    max_board_width: int | None,
) -> list[list[dict]]:
    uncovered_ids = {entry["id"] for entry in enabled_entries}
    chains: list[list[dict]] = []

    for seed in enabled_entries:
        if seed["id"] not in uncovered_ids:
            continue
        chain: list[dict] | None = None
        for length in range(target_length, 3, -1):
            chain = find_chain(
                seed=seed,
                char_index=char_index,
                target_length=length,
                preferred_ids=uncovered_ids,
                max_board_width=max_board_width,
            )
            if chain is not None:
                break
        if chain is None:
            raise RuntimeError(f"Unable to cover idiom {seed['id']} with a connected level.")
        chains.append(chain)
        for entry in chain:
            uncovered_ids.discard(entry["id"])

    if uncovered_ids:
        raise RuntimeError(f"Uncovered idioms remain after coverage build: {len(uncovered_ids)}")
    return chains


def build_character_index(catalog: list[dict]) -> dict[str, list[dict]]:
    index: dict[str, list[dict]] = defaultdict(list)
    for entry in catalog:
        for char in set(entry["text"]):
            index[char].append(entry)
    for char_entries in index.values():
        char_entries.sort(key=lambda entry: (entry["frequency_rank"], entry["text"]))
    return index


def find_chain(
    seed: dict,
    char_index: dict[str, list[dict]],
    target_length: int,
    *,
    preferred_ids: set[str] | None = None,
    max_board_width: int | None = None,
) -> list[dict] | None:
    chain = [seed]
    used_ids = {seed["id"]}

    def search() -> list[dict] | None:
        if len(chain) == target_length:
            return list(chain)

        for candidate in iter_neighbors(
            chain[-1],
            char_index,
            preferred_ids=preferred_ids,
            limit=96,
        ):
            if candidate["id"] in used_ids:
                continue
            chain.append(candidate)
            if try_layout(chain, max_board_width=max_board_width) is not None:
                used_ids.add(candidate["id"])
                result = search()
                if result is not None:
                    return result
                used_ids.remove(candidate["id"])
            chain.pop()
        return None

    return search()


def iter_neighbors(
    entry: dict,
    char_index: dict[str, list[dict]],
    *,
    preferred_ids: set[str] | None = None,
    limit: int = 48,
):
    seen_ids: set[str] = set()
    ranked_candidates: list[tuple[int, dict]] = []
    emission_order = 0
    for char in sorted(set(entry["text"]), key=lambda value: (len(char_index[value]), value)):
        for candidate in char_index[char]:
            if candidate["id"] == entry["id"] or candidate["id"] in seen_ids:
                continue
            seen_ids.add(candidate["id"])
            ranked_candidates.append((emission_order, candidate))
            emission_order += 1

    ranked_candidates.sort(
        key=lambda item: (
            0 if preferred_ids and item[1]["id"] in preferred_ids else 1,
            item[0],
            item[1]["frequency_rank"],
            item[1]["text"],
        )
    )
    for _, candidate in ranked_candidates[:limit]:
        yield candidate


def build_level(
    chain: list[dict],
    sequence: int,
    *,
    max_board_width: int | None = None,
) -> dict | None:
    placements = try_layout(chain, max_board_width=max_board_width)
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
        "layout_profile": f"compact-chain-{len(chain)}",
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


def try_layout(
    chain: list[dict],
    *,
    max_board_width: int | None = None,
) -> list[dict] | None:
    placements: list[dict] = [
        {
            "idiom_id": chain[0]["id"],
            "text": chain[0]["text"],
            "orientation": "across",
            "row": 0,
            "col": 0,
        }
    ]
    occupied: dict[tuple[int, int], str] = {}
    add_word(placements[0], occupied)

    def search(next_index: int) -> list[dict] | None:
        if next_index == len(chain):
            return [dict(placement) for placement in placements]

        options = find_placement_options(
            placements=placements,
            current=chain[next_index],
            occupied=occupied,
            max_board_width=max_board_width,
        )
        for placement in options:
            snapshot = dict(occupied)
            placements.append(placement)
            add_word(placement, occupied)
            result = search(next_index + 1)
            if result is not None:
                return result
            placements.pop()
            occupied.clear()
            occupied.update(snapshot)
        return None

    return search(1)


def find_placement_options(
    *,
    placements: list[dict],
    current: dict,
    occupied: dict[tuple[int, int], str],
    max_board_width: int | None,
    option_limit: int = 32,
) -> list[dict]:
    ranked_options: list[tuple[int, int, int, int, dict]] = []
    seen_signatures: set[tuple[str, int, int]] = set()

    for existing in placements:
        orientation = "down" if existing["orientation"] == "across" else "across"
        for existing_index, current_index in shared_char_pairs(existing["text"], current["text"]):
            intersection_row, intersection_col = cell_at(existing, existing_index)
            row = intersection_row - (current_index if orientation == "down" else 0)
            col = intersection_col - (current_index if orientation == "across" else 0)
            signature = (orientation, row, col)
            if signature in seen_signatures:
                continue
            if placement_conflicts(
                current["text"],
                row,
                col,
                orientation,
                occupied,
                (intersection_row, intersection_col),
            ):
                continue

            candidate = {
                "idiom_id": current["id"],
                "text": current["text"],
                "orientation": orientation,
                "row": row,
                "col": col,
            }
            width, height = measure_bounds(placements + [candidate])
            if max_board_width is not None and width > max_board_width:
                continue

            seen_signatures.add(signature)
            ranked_options.append((max(width, height), width * height, width + height, height, candidate))

    ranked_options.sort(key=lambda option: option[:4])
    return [candidate for *_, candidate in ranked_options[:option_limit]]


def measure_bounds(placements: list[dict]) -> tuple[int, int]:
    min_row = min_col = 10**9
    max_row = max_col = -(10**9)
    for placement in placements:
        text = placement["text"]
        for index, _ in enumerate(text):
            row, col = cell_at(placement, index)
            min_row = min(min_row, row)
            min_col = min(min_col, col)
            max_row = max(max_row, row)
            max_col = max(max_col, col)
    return (max_col - min_col) + 1, (max_row - min_row) + 1


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
