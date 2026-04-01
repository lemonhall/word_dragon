from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

from word_dragon_content.level_generator import generate_level_pack, write_levels


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--chapter-index", type=Path, required=True)
    parser.add_argument("--min-levels", type=int, required=True)
    parser.add_argument("--max-idioms-per-level", type=int, required=True)
    parser.add_argument("--preferred-idioms-per-level", type=int, default=4)
    parser.add_argument("--require-full-catalog-coverage", action="store_true")
    parser.add_argument("--max-board-width", type=int, default=None)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    catalog_payload = json.loads(args.catalog.read_text(encoding="utf-8"))
    catalog = catalog_payload["entries"] if isinstance(catalog_payload, dict) else catalog_payload
    levels, chapters = generate_level_pack(
        catalog=catalog,
        min_levels=args.min_levels,
        max_idioms_per_level=args.max_idioms_per_level,
        chapter_size=50,
        preferred_idioms_per_level=args.preferred_idioms_per_level,
        require_full_catalog_coverage=args.require_full_catalog_coverage,
        max_board_width=args.max_board_width,
    )

    write_levels(args.output_dir, levels)
    chapter_payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "strict": args.strict,
        "preferred_idioms_per_level": args.preferred_idioms_per_level,
        "require_full_catalog_coverage": args.require_full_catalog_coverage,
        "max_board_width": args.max_board_width,
        "chapters": chapters,
        "total_levels": len(levels),
    }
    args.chapter_index.parent.mkdir(parents=True, exist_ok=True)
    args.chapter_index.write_text(json.dumps(chapter_payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"generated levels: {len(levels)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
