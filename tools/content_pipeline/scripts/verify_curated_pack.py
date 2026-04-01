from __future__ import annotations

import argparse
import json
from pathlib import Path

from word_dragon_content.curated_pack import validate_curated_pack


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--chapter-index", type=Path, required=True)
    parser.add_argument("--levels-dir", type=Path, required=True)
    parser.add_argument("--expected-levels", type=int, required=True)
    args = parser.parse_args()

    errors, report = validate_curated_pack(
        workspace=args.workspace,
        catalog_path=args.catalog,
        chapter_index_path=args.chapter_index,
        levels_dir=args.levels_dir,
        expected_levels=args.expected_levels,
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        for error in errors:
            print(error)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
