from __future__ import annotations

import argparse
import json
from pathlib import Path

from word_dragon_content.layout_validator import default_layout_profile, validate_layout_bundle


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--chapter-index", type=Path, required=True)
    parser.add_argument("--levels-dir", type=Path, required=True)
    parser.add_argument("--min-cell-sp", type=int, required=True)
    parser.add_argument("--min-touch-dp", type=int, required=True)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    catalog_payload = json.loads(args.catalog.read_text(encoding="utf-8"))
    catalog = catalog_payload["entries"] if isinstance(catalog_payload, dict) else catalog_payload
    chapter_index = json.loads(args.chapter_index.read_text(encoding="utf-8"))
    errors, report = validate_layout_bundle(
        chapter_index=chapter_index,
        levels_dir=args.levels_dir,
        profile=default_layout_profile(),
        min_cell_sp=args.min_cell_sp,
        min_touch_dp=args.min_touch_dp,
        catalog=catalog,
    )

    if errors:
        for error in errors:
            print(error)
        return 1

    if args.strict:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print("layout validation passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
