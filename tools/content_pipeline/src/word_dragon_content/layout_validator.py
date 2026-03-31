from __future__ import annotations

import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True)
class UiLayoutProfile:
    min_board_cell_sp: int
    min_touch_target_dp: int
    primary_button_height_dp: int
    compact_button_height_dp: int
    comfortable_screen_padding_dp: int
    compact_screen_padding_dp: int
    comfortable_section_spacing_dp: int
    compact_section_spacing_dp: int
    board_weight: float
    controls_weight: float
    compact_padding_dp: int
    comfortable_padding_dp: int
    compact_spacing_dp: int
    comfortable_spacing_dp: int
    preferred_cell_size_dp: int
    minimum_readable_cell_size_dp: int
    portrait_screen_width_dp: int = 412
    portrait_screen_height_dp: int = 1024
    wide_screen_width_dp: int = 840
    compact_mode_height_threshold_dp: int = 820
    top_bar_height_dp: int = 64
    candidate_spacing_dp: int = 8


@dataclass(frozen=True)
class BoardLayoutMetrics:
    horizontal_padding_dp: int
    vertical_padding_dp: int
    cell_spacing_dp: int
    cell_size_dp: int
    requires_horizontal_scroll: bool

    def required_width_dp(self, board_width: int) -> int:
        return (
            (self.horizontal_padding_dp * 2)
            + (self.cell_size_dp * board_width)
            + (self.cell_spacing_dp * max(0, board_width - 1))
        )

    def required_height_dp(self, board_height: int) -> int:
        return (
            (self.vertical_padding_dp * 2)
            + (self.cell_size_dp * board_height)
            + (self.cell_spacing_dp * max(0, board_height - 1))
        )


def default_layout_profile() -> UiLayoutProfile:
    return UiLayoutProfile(
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


def validate_layout_bundle(
    chapter_index: dict,
    levels_dir: Path,
    profile: UiLayoutProfile,
    min_cell_sp: int,
    min_touch_dp: int,
    catalog: list[dict] | None = None,
) -> tuple[list[str], dict]:
    errors: list[str] = []
    levels = load_levels(levels_dir)
    chapter_level_ids = [level_id for chapter in chapter_index.get("chapters", []) for level_id in chapter.get("level_ids", [])]
    level_ids = [level["level_id"] for level in levels]

    if chapter_index.get("total_levels") != len(levels):
        errors.append(
            f"章节索引 total_levels={chapter_index.get('total_levels')} 与关卡文件数 {len(levels)} 不一致。"
        )
    if chapter_level_ids != level_ids:
        errors.append("章节索引里的关卡顺序与 levels 目录实际文件顺序不一致。")

    if profile.min_board_cell_sp < min_cell_sp:
        errors.append(f"棋盘字号下限只有 {profile.min_board_cell_sp}sp，低于要求的 {min_cell_sp}sp。")
    if profile.min_touch_target_dp < min_touch_dp:
        errors.append(f"最小触控尺寸只有 {profile.min_touch_target_dp}dp，低于要求的 {min_touch_dp}dp。")
    if profile.primary_button_height_dp < min_touch_dp:
        errors.append(f"主要操作按钮高度只有 {profile.primary_button_height_dp}dp，低于要求的 {min_touch_dp}dp。")
    if profile.compact_button_height_dp < min_touch_dp:
        errors.append(f"紧凑模式按钮高度只有 {profile.compact_button_height_dp}dp，低于要求的 {min_touch_dp}dp。")

    enabled_catalog_ids = {
        entry["id"]
        for entry in (catalog or [])
        if entry.get("enabled", True)
    }

    portrait_scroll_levels = 0
    horizontal_scroll_levels = 0
    smallest_board_cell_dp: int | None = None
    max_board_width = 0
    max_board_height = 0
    max_candidate_count = 0

    for level in levels:
        board_width = int(level["board_width"])
        board_height = int(level["board_height"])
        candidate_count = len(level.get("candidate_chars", []))
        max_board_width = max(max_board_width, board_width)
        max_board_height = max(max_board_height, board_height)
        max_candidate_count = max(max_candidate_count, candidate_count)

        if enabled_catalog_ids:
            for idiom_id in level.get("idiom_ids", []):
                if idiom_id not in enabled_catalog_ids:
                    errors.append(f"{level['level_id']} 引用了未启用或不存在的词条 {idiom_id}。")

        portrait_metrics = calculate_board_layout(
            available_width_dp=profile.portrait_screen_width_dp - (compact_page_padding(level, profile) * 2),
            board_width=board_width,
            profile=profile,
        )
        smallest_board_cell_dp = (
            portrait_metrics.cell_size_dp
            if smallest_board_cell_dp is None
            else min(smallest_board_cell_dp, portrait_metrics.cell_size_dp)
        )

        if portrait_metrics.cell_size_dp < profile.minimum_readable_cell_size_dp:
            errors.append(
                f"{level['level_id']} 的棋盘格尺寸只有 {portrait_metrics.cell_size_dp}dp，低于可读下限 "
                f"{profile.minimum_readable_cell_size_dp}dp。"
            )
        if portrait_metrics.requires_horizontal_scroll:
            horizontal_scroll_levels += 1
            errors.append(f"{level['level_id']} 的棋盘在竖屏下需要横向滚动。")

        portrait_height_budget = estimate_portrait_height_budget(level, profile)
        portrait_content_height = estimate_portrait_content_height(level, portrait_metrics, profile, min_touch_dp)
        if portrait_content_height > portrait_height_budget:
            portrait_scroll_levels += 1

    report = {
        "profile": asdict(profile),
        "total_levels": len(levels),
        "max_board_width": max_board_width,
        "max_board_height": max_board_height,
        "max_candidate_count": max_candidate_count,
        "smallest_board_cell_dp": smallest_board_cell_dp or 0,
        "portrait_scroll_levels": portrait_scroll_levels,
        "horizontal_scroll_levels": horizontal_scroll_levels,
    }
    return errors, report


def compact_page_padding(level: dict, profile: UiLayoutProfile) -> int:
    return (
        profile.compact_screen_padding_dp
        if is_compact_mode(level=level, profile=profile)
        else profile.comfortable_screen_padding_dp
    )


def calculate_board_layout(
    available_width_dp: int,
    board_width: int,
    profile: UiLayoutProfile,
) -> BoardLayoutMetrics:
    if available_width_dp <= 0:
        raise ValueError("available_width_dp must be positive")
    if board_width <= 0:
        raise ValueError("board_width must be positive")

    use_compact_layout = board_width >= 6 or available_width_dp <= 320
    horizontal_padding_dp = profile.compact_padding_dp if use_compact_layout else profile.comfortable_padding_dp
    vertical_padding_dp = horizontal_padding_dp
    cell_spacing_dp = profile.compact_spacing_dp if use_compact_layout else profile.comfortable_spacing_dp
    available_board_width_dp = available_width_dp - (horizontal_padding_dp * 2) - (cell_spacing_dp * max(0, board_width - 1))
    fitted_cell_size_dp = math.floor(available_board_width_dp / board_width)
    cell_size_dp = max(profile.minimum_readable_cell_size_dp, min(fitted_cell_size_dp, profile.preferred_cell_size_dp))
    metrics = BoardLayoutMetrics(
        horizontal_padding_dp=horizontal_padding_dp,
        vertical_padding_dp=vertical_padding_dp,
        cell_spacing_dp=cell_spacing_dp,
        cell_size_dp=cell_size_dp,
        requires_horizontal_scroll=False,
    )
    required_width_dp = metrics.required_width_dp(board_width)
    return BoardLayoutMetrics(
        horizontal_padding_dp=horizontal_padding_dp,
        vertical_padding_dp=vertical_padding_dp,
        cell_spacing_dp=cell_spacing_dp,
        cell_size_dp=cell_size_dp,
        requires_horizontal_scroll=required_width_dp > available_width_dp,
    )


def estimate_portrait_content_height(
    level: dict,
    portrait_metrics: BoardLayoutMetrics,
    profile: UiLayoutProfile,
    min_touch_dp: int,
) -> int:
    compact_mode = is_compact_mode(level=level, profile=profile)
    page_padding_dp = profile.compact_screen_padding_dp if compact_mode else profile.comfortable_screen_padding_dp
    section_spacing_dp = profile.compact_section_spacing_dp if compact_mode else profile.comfortable_section_spacing_dp
    button_height_dp = max(
        profile.compact_button_height_dp if compact_mode else profile.primary_button_height_dp,
        min_touch_dp,
    )
    card_padding_dp = profile.compact_padding_dp if compact_mode else profile.comfortable_padding_dp
    board_height_dp = portrait_metrics.required_height_dp(int(level["board_height"]))
    candidate_rows = estimate_candidate_rows(level=level, profile=profile, card_padding_dp=card_padding_dp, button_height_dp=button_height_dp)
    candidate_card_height_dp = (card_padding_dp * 2) + (candidate_rows * button_height_dp) + (
        profile.candidate_spacing_dp * max(0, candidate_rows - 1)
    )
    action_rows_height_dp = (button_height_dp * 2) + profile.candidate_spacing_dp
    return (
        profile.top_bar_height_dp
        + (page_padding_dp * 2)
        + board_height_dp
        + section_spacing_dp
        + candidate_card_height_dp
        + section_spacing_dp
        + action_rows_height_dp
    )


def estimate_portrait_height_budget(level: dict, profile: UiLayoutProfile) -> int:
    compact_mode = is_compact_mode(level=level, profile=profile)
    # 留出少量系统栏余量，贴近老年用户常见的大字号真机。
    safety_margin_dp = 48 if compact_mode else 64
    return profile.portrait_screen_height_dp - safety_margin_dp


def estimate_candidate_rows(
    level: dict,
    profile: UiLayoutProfile,
    card_padding_dp: int,
    button_height_dp: int,
) -> int:
    del button_height_dp
    candidate_count = len(level.get("candidate_chars", []))
    if candidate_count == 0:
        return 0

    content_width_dp = profile.portrait_screen_width_dp - (compact_page_padding(level, profile) * 2)
    usable_width_dp = max(1, content_width_dp - (card_padding_dp * 2))
    min_button_width_dp = profile.min_touch_target_dp
    columns = max(
        1,
        (usable_width_dp + profile.candidate_spacing_dp) // (min_button_width_dp + profile.candidate_spacing_dp),
    )
    return math.ceil(candidate_count / columns)


def is_compact_mode(level: dict, profile: UiLayoutProfile) -> bool:
    return (
        profile.portrait_screen_height_dp <= profile.compact_mode_height_threshold_dp
        or int(level["board_height"]) >= 6
        or len(level.get("candidate_chars", [])) >= 10
    )


def load_levels(levels_dir: Path) -> list[dict]:
    levels: list[dict] = []
    for path in sorted(levels_dir.glob("*.json")):
        levels.append(json.loads(path.read_text(encoding="utf-8")))
    return levels
