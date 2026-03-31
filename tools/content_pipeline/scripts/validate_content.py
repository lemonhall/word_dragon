from __future__ import annotations

import argparse
import json
from pathlib import Path

from word_dragon_content.content_validator import validate_content_bundle


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--chapter-index", type=Path, required=True)
    parser.add_argument("--levels-dir", type=Path, required=True)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    catalog_payload = json.loads(args.catalog.read_text(encoding="utf-8"))
    catalog = catalog_payload["entries"] if isinstance(catalog_payload, dict) else catalog_payload
    chapter_index = json.loads(args.chapter_index.read_text(encoding="utf-8"))
    min_levels = 1000 if args.strict else 1
    errors = validate_content_bundle(
        catalog=catalog,
        chapter_index=chapter_index,
        levels_dir=args.levels_dir,
        min_levels=min_levels,
        max_idioms_per_level=8,
    )
    if errors:
        for error in errors:
            print(error)
        return 1
    print("content validation passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
